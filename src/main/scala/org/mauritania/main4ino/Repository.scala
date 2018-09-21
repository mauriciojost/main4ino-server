package org.mauritania.main4ino

import cats.implicits._
import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie._
import doobie.implicits._
import doobie.free.connection.raw
import org.mauritania.main4ino.models.Device.Metadata
import org.mauritania.main4ino.models._
import fs2.Stream
import org.mauritania.main4ino.Repository.Table.Table

// Naming regarding to SQL
class Repository(transactor: Transactor[IO]) {

  def insertDevice(table: Table, t: Device): IO[RecordId] = {
    val transaction = for {
      deviceId <- sqlInsertMetadata(table, t.metadata)
      nroTargetActorProps <- sqlInsertActorTup(table, t.asActorTups, deviceId)
    } yield (deviceId)
    transaction.transact(transactor)
  }

  def selectDeviceWhereRequestId(table: Table, requestId: RecordId): IO[Option[Device]] = {
    val transaction = for {
      t <- sqlSelectMetadataWhereRequestId(table, requestId)
      p <- sqlSelectActorTupWhereRequestIdActorStatus(table, requestId)
    } yield (t.map(Device.fromActorTups(_, p)))
    transaction.transact(transactor)
  }

  def selectDevicesWhereTimestamp(table: Table, device: DeviceName, from: Option[Timestamp], to: Option[Timestamp]): IO[Iterable[Device]] = {
    val transaction = for {
      d <- sqlSelectMetadataActorTupWhereDevice(table, device, from, to)
    } yield (d)
    val s = transaction.transact(transactor)
    val iol = s.compile.toList
    iol.map(l => Device.fromDevice1s(l))
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

  def selectMaxActorTupsStatus(table: Table, device: DeviceName, actor: ActorName, status: Option[Status]): IO[List[ActorTup]] = {
    // TODO investigate if this yields only one sql query in the end, otherwise write it as such
    val ac = Some(actor)
    val transaction = for {
      i <- sqlSelectLastRequestIdWhereDeviceActorStatus(table, device, ac, status)
      p <- i.map(sqlSelectActorTupWhereRequestIdActorStatus(table, _, ac, status)).getOrElse(raw[List[ActorTup]](x => List.empty[ActorTup]))
    } yield (p)
    transaction.transact(transactor)
  }

  def selectActorTupWhereDeviceActorStatus(table: Table, device: DeviceName, actor: Option[ActorName], status: Option[Status], consume: Boolean): Stream[IO, ActorTup] = {
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
    val sql = s"INSERT INTO ${table.code} (request_id, device_name, actor_name, property_name, property_value, property_status) VALUES (?, ?, ?, ?, ?, ?)"
    Update[ActorTup](sql).updateMany(t.toList.map(_.withRequestId(Some(requestId))))
  }

  private def sqlInsertMetadata(table: Table, m: Metadata): ConnectionIO[RecordId] = {
    (fr"INSERT INTO " ++ Fragment.const(table.code + "_requests") ++ fr" (creation, device_name) VALUES (${m.timestamp}, ${m.device})")
      .update.withUniqueGeneratedKeys[RecordId]("id")
  }

  private def sqlSelectMetadataActorTupWhereDevice(table: Table, d: DeviceName, from: Option[Timestamp], to: Option[Timestamp]): Stream[ConnectionIO, Device.Device1] = {
    val fromFr = from match {
      case Some(a) => fr"AND r.creation >= $a"
      case None => fr""
    }
    val toFr = to match {
      case Some(a) => fr"AND r.creation <= $a"
      case None => fr""
    }
    (fr"SELECT r.id, r.creation, r.device_name, t.request_id, t.device_name, t.actor_name, t.property_name, t.property_value, t.property_status" ++
      fr"FROM" ++ Fragment.const(table.code + "_requests") ++ fr"as r JOIN" ++ Fragment.const(table.code) ++ fr"as t" ++
      fr"ON r.id = t.request_id" ++
      fr"WHERE r.device_name=$d" ++ fromFr ++ toFr)
      .query[Device.Device1].stream
  }

  private def sqlSelectMetadataWhereRequestId(table: Table, id: RecordId): ConnectionIO[Option[Metadata]] = {
    (fr"SELECT id, creation, device_name FROM " ++ Fragment.const(table.code + "_requests") ++ fr" WHERE id=$id")
      .query[Metadata].option
  }

  private def sqlSelectLastRequestIdWhereDeviceActorStatus(table: Table, device: DeviceName, actor: Option[ActorName], status: Option[Status]): ConnectionIO[Option[RecordId]] = {
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
    status: Option[Status] = None
  ): ConnectionIO[List[ActorTup]] = {
    val actorFr = actor match {
      case Some(a) => fr"AND actor_name = $a"
      case None => fr""
    }
    val statusFr = status match {
      case Some(a) => fr"AND property_status = $a"
      case None => fr""
    }
    (fr"SELECT request_id, device_name, actor_name, property_name, property_value, property_status FROM " ++ Fragment.const(table.code) ++ fr" WHERE request_id=$requestId" ++ actorFr ++ statusFr)
      .query[ActorTup].accumulate
  }

  private def sqlUpdateActorTupWhereDeviceActorConsume(table: Table, device: DeviceName, actor: Option[ActorName]): ConnectionIO[Int] = {
    val actorFr = actor match {
      case Some(a) => fr"AND actor_name = $a"
      case None => fr""
    }
    (fr"UPDATE " ++ Fragment.const(table.code) ++ fr" SET property_status = ${Status.Consumed} WHERE property_status=${Status.Created} and device_name=$device" ++ actorFr)
      .update.run
  }

  private def sqlSelectActorTupWhereDeviceActorStatus(
    table: Table,
    device: DeviceName,
    actor: Option[ActorName],
    status: Option[Status]
  ): Stream[ConnectionIO, ActorTup] = {

    val actorFr = actor match {
      case Some(a) => fr"AND actor_name = $a"
      case None => fr""
    }
    val statusFr = status match {
      case Some(a) => fr"AND property_status = $a"
      case None => fr""
    }
    (fr"SELECT request_id, device_name, actor_name, property_name, property_value, property_status FROM " ++ Fragment.const(table.code) ++ fr" WHERE device_name=$device" ++ actorFr ++ statusFr).query[ActorTup].stream
  }

}

object Repository {

  object Table {

    sealed abstract class Table(val code: String)
    case object Reports extends Table("reports")
    case object Targets extends Table("targets")

    val all = List(Reports, Targets)
    def resolve(s: String): Option[Table] = all.find(_.code == s)
  }

}