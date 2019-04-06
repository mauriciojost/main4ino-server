package org.mauritania.main4ino

import cats.effect.IO
import cats.free.Free
import cats.implicits._
import doobie._
import doobie.free.connection.{ConnectionOp, raw}
import doobie.implicits._
import doobie.util.transactor.Transactor
import fs2.Stream
import org.mauritania.main4ino.RepositoryIO.Table.Table
import org.mauritania.main4ino.helpers.Time
import org.mauritania.main4ino.models.ActorTup.{Status => AtStatus}
import org.mauritania.main4ino.models.Device.Metadata.{Status => MdStatus}
import org.mauritania.main4ino.models.Device.Metadata
import org.mauritania.main4ino.models.PropsMap.PropsMap
import org.mauritania.main4ino.models._

trait Repository[F[_]] {

  type ErrMsg = String

  def insertDeviceActor(table: Table, device: DeviceName, actor: ActorName, requestId: RecordId, r: PropsMap, ts: EpochSecTimestamp): F[Either[ErrMsg, Int]]

  def cleanup(table: Table, now: EpochSecTimestamp, preserveWindowSecs: Int): F[Int]

  def deleteDeviceWhereName(table: Table, device: DeviceName): F[Int]

  def insertDevice(table: Table, t: Device): F[RecordId]

  def selectDeviceWhereRequestId(table: Table, dev: DeviceName, requestId: RecordId): F[Either[ErrMsg, Device]]

  def selectDevicesWhereTimestamp(table: Table, device: DeviceName, from: Option[EpochSecTimestamp], to: Option[EpochSecTimestamp]): F[Iterable[Device]]

  def selectMaxDevice(table: Table, device: DeviceName): F[Option[Device]]

  def selectMaxActorTupsStatus(table: Table, device: DeviceName, actor: ActorName, status: Option[AtStatus]): F[List[ActorTup]]

  def selectActorTupWhereDeviceActorStatus(table: Table, device: DeviceName, actor: Option[ActorName], status: Option[AtStatus], consume: Boolean): Stream[F, ActorTup]

  def selectRequestIdsWhereDevice(table: Table, d: DeviceName): Stream[F, RecordId]

}

// Naming regarding to SQL
class RepositoryIO(transactor: Transactor[IO]) extends Repository[IO] {

  def cleanup(table: Table, now: EpochSecTimestamp, retentionSecs: Int) = {
    val transaction = for {
      m <- sqlDeleteMetadataWhereCreationIsLess(table, now - retentionSecs)
      _ <- sqlDeleteActorTupOrphanOfRequest(table)
    } yield (m)
    transaction.transact(transactor)
  }

  def deleteDeviceWhereName(table: Table, device: String) = {
    val transaction = for {
      m <- sqlDeleteMetadataWhereDeviceName(table, device)
      _ <- sqlDeleteActorTupOrphanOfRequest(table)
    } yield (m)
    transaction.transact(transactor)
  }

  def insertDevice(table: Table, t: Device): IO[RecordId] = { // TODO can't it be done all in one single sql query?
    val transaction = for {
      deviceId <- sqlInsertMetadata(table, t.metadata)
      nroTargetActorProps <- sqlInsertActorTup(table, t.asActorTups, deviceId)
    } yield (deviceId)
    transaction.transact(transactor)
  }

  def insertDeviceActor(table: Table, dev: DeviceName, actor: ActorName, requestId: RecordId, p: PropsMap, ts: EpochSecTimestamp): IO[Either[ErrMsg, Int]] = {
    val transaction = for {
      mtd <- sqlSelectMetadataWhereRequestId(table, requestId)
      ok = mtd.exists(m => m.device == dev && m.status == MdStatus.Open)
      r: ConnectionIO[Either[ErrMsg, Int]] = if (ok)
        sqlInsertActorTup(table, ActorTup.fromPropsMap(requestId, dev, actor, p, ts), requestId).map(Right.apply[ErrMsg, Int])
      else
        Free.pure[ConnectionOp, Either[ErrMsg, Int]](Left.apply[ErrMsg, Int](s"Rejected: request $requestId does not relate to $dev or is closed"))
      inserts <- r
    } yield (inserts)
    transaction.transact(transactor)
  }

  def selectDeviceWhereRequestId(table: Table, dev: DeviceName, requestId: RecordId): IO[Either[ErrMsg, Device]] = {
    val transaction = for {
      t <- sqlSelectMetadataWhereRequestId(table, requestId)
      k = t.exists(_.device == dev)
      p <- sqlSelectActorTupWhereRequestIdActorStatus(table, requestId)
      j = t.map(Device.fromActorTups(_, p)).filter(_.metadata.device == dev).toRight(s"Request $requestId does not belong to $dev")
    } yield (j)
    transaction.transact(transactor)
  }

  def selectDevicesWhereTimestamp(table: Table, device: DeviceName, from: Option[EpochSecTimestamp], to: Option[EpochSecTimestamp]): IO[Iterable[Device]] = {
    val transaction = for {
      d <- sqlSelectMetadataActorTupWhereDevice(table, device, from, to)
    } yield (d)
    val s = transaction.transact(transactor)
    val iol = s.compile.toList
    iol.map(l => Device.fromDevice1s(l).toSeq.sortBy(_.metadata.creation))
  }

  def selectMaxDevice(table: Table, device: DeviceName): IO[Option[Device]] = {
    val transaction = for {
      i <- sqlSelectLastRequestIdWhereDeviceActorStatus(table, device, None, None)
      t <- i.map(sqlSelectMetadataWhereRequestId(table, _)).getOrElse(raw[Option[Metadata]](x => None))
      p <- i.map(sqlSelectActorTupWhereRequestIdActorStatus(table, _)).getOrElse(raw[List[ActorTup]](x => List.empty[ActorTup]))
    } yield {
      (t, p) match {
        case (Some(m), l) => Some(Device.fromActorTups(m, l))
        case _ => None

      }
    }
    transaction.transact(transactor)
  }

  def selectMaxActorTupsStatus(table: Table, device: DeviceName, actor: ActorName, status: Option[AtStatus]): IO[List[ActorTup]] = {
    // TODO investigate if this yields only one sql query in the end, otherwise write it as such
    val ac = Some(actor)
    val transaction = for {
      i <- sqlSelectLastRequestIdWhereDeviceActorStatus(table, device, ac, status)
      p <- i.map(sqlSelectActorTupWhereRequestIdActorStatus(table, _, ac, status)).getOrElse(raw[List[ActorTup]](x => List.empty[ActorTup]))
    } yield (p)
    transaction.transact(transactor)
  }

  def selectActorTupWhereDeviceActorStatus(table: Table, device: DeviceName, actor: Option[ActorName], status: Option[AtStatus], consume: Boolean): Stream[IO, ActorTup] = {
    val transaction = for {
      p <- sqlSelectActorTupWhereDeviceActorStatus(table, device, actor, status)
      c <- if (consume) Stream.eval[ConnectionIO, Int](sqlUpdateActorTupWhereDeviceActorConsume(table, device, actor)) else Stream.fromIterator[ConnectionIO, Int](Iterator(0))
    } yield (p)
    transaction.transact(transactor)
  }

  def selectRequestIdsWhereDevice(table: Table, d: DeviceName): Stream[IO, RecordId] = {
    (fr"SELECT id FROM " ++ Fragment.const(table.code + "_requests") ++ fr" WHERE device_name=$d").query[RecordId].stream.transact(transactor)
  }

  // SQL queries (private)

  private def sqlInsertActorTup(table: Table, t: Iterable[ActorTup], requestId: RecordId): ConnectionIO[Int] = {
    val sql = s"INSERT INTO ${table.code} (request_id, device_name, actor_name, property_name, property_value, property_status, creation) VALUES (?, ?, ?, ?, ?, ?, ?)"
    Update[ActorTup](sql).updateMany(t.toList.map(_.withRequestId(Some(requestId))))
  }

  private def sqlInsertMetadata(table: Table, m: Metadata): ConnectionIO[RecordId] = {
    (fr"INSERT INTO " ++ Fragment.const(table.code + "_requests") ++ fr" (creation, device_name, status) VALUES (${m.creation}, ${m.device}, ${m.status})")
      .update.withUniqueGeneratedKeys[RecordId]("id")
  }

  private def sqlDeleteMetadataWhereDeviceName(table: Table, device: DeviceName): ConnectionIO[Int] = {
    (fr"DELETE FROM" ++ Fragment.const(table.code + "_requests") ++ fr"WHERE device_name=$device")
      .update.run
  }

  private def sqlDeleteMetadataWhereCreationIsLess(table: Table, upperbound: EpochSecTimestamp): ConnectionIO[Int] = {
    (fr"DELETE FROM" ++ Fragment.const(table.code + "_requests") ++ fr"WHERE creation < $upperbound")
      .update.run
  }

  private def sqlDeleteActorTupOrphanOfRequest(table: Table): ConnectionIO[Int] = {
    (fr"DELETE FROM " ++ Fragment.const(table.code) ++ fr" tu WHERE NOT EXISTS (SELECT 1 FROM " ++ Fragment.const(table.code + "_requests") ++ fr" re where tu.request_id = re.id)")
      .update.run
  }

  private def sqlSelectMetadataActorTupWhereDevice(table: Table, d: DeviceName, from: Option[EpochSecTimestamp], to: Option[EpochSecTimestamp]): Stream[ConnectionIO, Device.Device1] = {
    val fromFr = from match {
      case Some(a) => fr"AND r.creation >= $a"
      case None => fr""
    }
    val toFr = to match {
      case Some(a) => fr"AND r.creation <= $a"
      case None => fr""
    }
    (fr"SELECT r.id, r.creation, r.device_name, r.status, t.request_id, t.device_name, t.actor_name, t.property_name, t.property_value, t.property_status, t.creation" ++
      fr"FROM" ++ Fragment.const(table.code + "_requests") ++ fr"as r JOIN" ++ Fragment.const(table.code) ++ fr"as t" ++
      fr"ON r.id = t.request_id" ++
      fr"WHERE r.device_name=$d" ++ fromFr ++ toFr)
      .query[Device.Device1].stream
  }

  private def sqlSelectMetadataWhereRequestId(table: Table, id: RecordId): ConnectionIO[Option[Metadata]] = {
    (fr"SELECT id, creation, device_name, status FROM " ++ Fragment.const(table.code + "_requests") ++ fr" WHERE id=$id")
      .query[Metadata].option
  }

  private def sqlSelectLastRequestIdWhereDeviceActorStatus(table: Table, device: DeviceName, actor: Option[ActorName], status: Option[AtStatus]): ConnectionIO[Option[RecordId]] = {
    val actorFr = actor match {
      case Some(a) => fr"AND actor_name = $a"
      case None => fr""
    }
    val statusFr = status match {
      case Some(a) => fr"AND property_status = $a"
      case None => fr""
    }
    (fr"SELECT MAX(request_id) FROM " ++ Fragment.const(table.code) ++ fr" WHERE device_name=$device" ++ actorFr ++ statusFr).query[Option[RecordId]].unique
  }

  private def sqlSelectActorTupWhereRequestIdActorStatus(
    table: Table,
    requestId: RecordId,
    actor: Option[ActorName] = None,
    status: Option[AtStatus] = None
  ): ConnectionIO[List[ActorTup]] = {
    val actorFr = actor match {
      case Some(a) => fr"AND actor_name = $a"
      case None => fr""
    }
    val statusFr = status match {
      case Some(a) => fr"AND property_status = $a"
      case None => fr""
    }
    (fr"SELECT request_id, device_name, actor_name, property_name, property_value, property_status, creation FROM " ++ Fragment.const(table.code) ++ fr" WHERE request_id=$requestId" ++ actorFr ++ statusFr)
      .query[ActorTup].accumulate
  }

  private def sqlUpdateActorTupWhereDeviceActorConsume(table: Table, device: DeviceName, actor: Option[ActorName]): ConnectionIO[Int] = {
    val actorFr = actor match {
      case Some(a) => fr"AND actor_name = $a"
      case None => fr""
    }
    (fr"UPDATE " ++ Fragment.const(table.code) ++ fr" SET property_status = ${AtStatus.Consumed} WHERE property_status=${AtStatus.Created} and device_name=$device" ++ actorFr)
      .update.run
  }

  private def sqlSelectActorTupWhereDeviceActorStatus(
    table: Table,
    device: DeviceName,
    actor: Option[ActorName],
    status: Option[AtStatus]
  ): Stream[ConnectionIO, ActorTup] = {

    val actorFr = actor match {
      case Some(a) => fr"AND actor_name = $a"
      case None => fr""
    }
    val statusFr = status match {
      case Some(a) => fr"AND property_status = $a"
      case None => fr""
    }
    (fr"SELECT request_id, device_name, actor_name, property_name, property_value, property_status, creation FROM " ++ Fragment.const(table.code) ++ fr" WHERE device_name=$device" ++ actorFr ++ statusFr).query[ActorTup].stream
  }

}

object RepositoryIO {

  object Table {

    sealed abstract class Table(val code: String)

    case object Reports extends Table("reports")

    case object Targets extends Table("targets")

    val all = List(Reports, Targets)

    def resolve(s: String): Option[Table] = all.find(_.code == s)
  }

}