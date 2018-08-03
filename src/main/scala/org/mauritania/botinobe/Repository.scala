package org.mauritania.botinobe

import cats.implicits._
import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie._
import doobie.implicits._
import org.mauritania.botinobe.models.Device.Metadata
import org.mauritania.botinobe.models._
import fs2.Stream
import org.mauritania.botinobe.Repository.Table.Table

// Naming regarding to SQL
class Repository(transactor: Transactor[IO]) {

  def insertDevice(table: Table, t: Device): IO[RecordId] = {
    val transaction = for {
      deviceId <- sqlInsertMetadata(table, t.metadata)
      nroTargetActorProps <- sqlInsertActorTup(table, t.asActorTups, deviceId)
    } yield (deviceId)
    transaction.transact(transactor)
  }

  def selectDeviceWhereRequestId(table: Table, requestId: RecordId): IO[Device] = {
    val transaction = for {
      t <- sqlSelectMetadataWhereRequestId(table, requestId)
      p <- sqlSelectActorTupWhereRequestId(table, requestId)
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }

  def selectDevices(table: Table, device: DeviceName): Stream[IO, Device] = {
    val transaction = for {
      t <- sqlSelectMetadataWhereDevice(table, device)
      p <- Stream.eval(sqlSelectActorTupWhereRequestId(table, t.id.get)) // must exist or better fail
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }

  def selectMaxDevice(table: Table, device: DeviceName): IO[Device] = {
    val transaction = for {
      i <- sqlSelectLastRequestIdWhereDevice(table, device)
      t <- sqlSelectMetadataWhereRequestId(table, i)
      p <- sqlSelectActorTupWhereRequestId(table, i)
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }

  def selectActorTupWhereDeviceActorStatus(table: Table, device: DeviceName, actor: Option[ActorName], status: Status, consume: Boolean): Stream[IO, ActorTup] = {
    val transaction = for {
      p <- sqlSelectActorTupWhereDeviceActorStatus(table, device, actor, status)
      c <- if (consume) Stream.eval[ConnectionIO, Int](sqlUpdateActorTupWhereStatusDeviceActor(table, device, actor, Status.Created, Status.Consumed)) else Stream.fromIterator[ConnectionIO, Int](Iterator(0))
    } yield (p)
    transaction.transact(transactor)
  }

  def selectRequestIdsWhereDevice(table: Table, d: DeviceName): Stream[IO, RecordId] = {
    (fr"SELECT id FROM " ++ Fragment.const(table.code + "_requests") ++ fr" WHERE device_name=$d").query[RecordId].stream.transact(transactor)
  }

  private def sqlInsertActorTup(table: Table, t: Iterable[ActorTup], requestId: RecordId): ConnectionIO[Int] = {
    val sql = s"INSERT INTO ${table.code} (request_id, device_name, actor_name, property_name, property_value, property_status) VALUES (?, ?, ?, ?, ?, ?)"
    Update[ActorTup](sql).updateMany(t.toList.map(_.withRequestId(Some(requestId))))
  }

  private def sqlInsertMetadata(table: Table, m: Metadata): ConnectionIO[RecordId] = {
    (fr"INSERT INTO " ++ Fragment.const(table.code + "_requests") ++ fr" (creation, device_name) VALUES (${m.timestamp}, ${m.device})")
      .update.withUniqueGeneratedKeys[RecordId]("id")
  }

  private def sqlSelectMetadataWhereDevice(table: Table, d: DeviceName): Stream[ConnectionIO, Metadata] = {
    (fr"SELECT id, creation, device_name FROM " ++ Fragment.const(table.code + "_requests") ++ fr" WHERE device_name=$d")
      .query[Metadata].stream
  }

  private def sqlSelectMetadataWhereRequestId(table: Table, id: RecordId): ConnectionIO[Metadata] = {
    (fr"SELECT id, creation, device_name FROM " ++ Fragment.const(table.code + "_requests") ++ fr" WHERE id=$id")
      .query[Metadata].unique
  }

  private def sqlSelectLastRequestIdWhereDevice(table: Table, device: DeviceName): ConnectionIO[RecordId] = {
    (fr"SELECT MAX(id) FROM " ++ Fragment.const(table.code + "_requests") ++ fr" WHERE device_name=$device").query[RecordId].unique
  }

  private def sqlSelectActorTupWhereRequestId(table: Table, requestId: RecordId): ConnectionIO[List[ActorTup]] = {
    (fr"SELECT request_id, device_name, actor_name, property_name, property_value, property_status FROM " ++ Fragment.const(table.code) ++ fr" WHERE request_id=$requestId")
      .query[ActorTup].accumulate
  }

  private def sqlUpdateActorTupWhereStatusDeviceActor(table: Table, device: DeviceName, actor: Option[ActorName], status: Status, newStatus: Status): ConnectionIO[Int] = {
    val actorFr = actor match {
      case Some(a) => fr"AND actor_name = $a"
      case None => fr""
    }
    (fr"UPDATE " ++ Fragment.const(table.code) ++ fr" SET property_status = ${newStatus} WHERE property_status=$status and device_name=$device" ++ actorFr)
      .update.run
  }

  private def sqlSelectActorTupWhereDeviceActorStatus(table: Table, device: DeviceName, actor: Option[ActorName], status: Status): Stream[ConnectionIO, ActorTup] = {
    val actorFr = actor match {
      case Some(a) => fr"AND actor_name = $a"
      case None => fr""
    }
    (fr"SELECT request_id, device_name, actor_name, property_name, property_value, property_status FROM " ++ Fragment.const(table.code) ++ fr" WHERE property_status=$status and device_name=$device " ++ actorFr).query[ActorTup].stream
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