package org.mauritania.botinobe

import cats.implicits._
import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie._
import doobie.implicits._
import org.mauritania.botinobe.models.Device.Metadata
import org.mauritania.botinobe.models._
import fs2.Stream
import org.mauritania.botinobe.Repository.Table
import org.mauritania.botinobe.Repository.Table.Table

// Naming regarding to CRUD
class Repository(transactor: Transactor[IO]) {


  def createDevice(table: Table, t: Device): IO[RecordId] = {
    val transaction = for {
      deviceId <- sqlDeviceIn_X_requests(table, t)
      nroTargetActorProps <- sqlActorTupsIn_X(table, t.asActorTups, deviceId)
    } yield (deviceId)
    transaction.transact(transactor)
  }

  def readDevice(table: Table, requestId: RecordId): IO[Device] = {
    val transaction = for {
      t <- sqlMetadataFromRequestId_X_requests(table, requestId)
      p <- sqlActorTupsFromRequestId_X(table, requestId)
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }

  def readLastDevice(table: Table, device: DeviceName): IO[Device] = {
    val transaction = for {
      i <- sqlRequestIdFromDeviceLast_X_requests(table, device)
      t <- sqlMetadataFromRequestId_X_requests(table, i)
      p <- sqlActorTupsFromRequestId_X(table, i)
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }

  def readPropsConsume(table: Table, device: DeviceName, actor: ActorName, status: Status): Stream[IO, Device] = {
    val transaction = for {
      pi <- sqlPropIdsFromDeviceNameActorNameStatus_X(table, device, actor, status)
      c <- Stream.eval(sqlChangeStatusFromPropId_X(table, pi, Status.Consumed))
      t <- Stream.eval(sqlActorTupsFromPropId_X(table, pi))
    } yield (Device.fromActorTups(t))
    transaction.transact(transactor)
  }

  def readProps(table: Table, device: DeviceName, actor: ActorName, status: Status): Stream[IO, Device] = {
    val transaction = for {
      pi <- sqlPropIdsFromDeviceNameActorNameStatus_X(table, device, actor, status)
      t <- Stream.eval(sqlActorTupsFromPropId_X(table, pi))
    } yield (Device.fromActorTups(t))
    transaction.transact(transactor)
  }

  def readRequestIds(table: Table, deviceName: DeviceName): Stream[IO, RecordId] = {
    sqlRequestIdFromDeviceName_X_requests(table, deviceName).transact(transactor)
  }

  def readDevicePropIdsWhereDeviceActorStatus(table: Table, device: DeviceName, actor: ActorName, status: Status): Stream[IO, RecordId] = {
      sqlPropIdsFromDeviceNameActorNameStatus_X(table: Table, device, actor, status).transact (transactor)
  }

  private def sqlActorTupsIn_X(table: Table, t: Iterable[ActorTup], requestId: RecordId): ConnectionIO[Int] = {
    val sql = s"INSERT INTO ${table.code} (request_id, device_name, actor_name, property_name, property_value, property_status) VALUES (?, ?, ?, ?, ?, ?)"
    Update[ActorTup](sql).updateMany(t.toList.map(_.withId(Some(requestId))))
  }

  private def sqlDeviceIn_X_requests(table: Table, t: Device): ConnectionIO[RecordId] = {
    (fr"INSERT INTO " ++ Fragment.const(table.code + "_requests") ++ fr" (creation, device_name) VALUES (${t.metadata.timestamp}, ${t.metadata.device})")
      .update.withUniqueGeneratedKeys[RecordId]("id")
  }

  private def sqlMetadataFromRequestId_X_requests(table: Table, id: RecordId): ConnectionIO[Metadata] = {
    (fr"SELECT id, creation, device_name FROM " ++ Fragment.const(table.code + "_requests") ++ fr" WHERE id=$id")
      .query[Metadata].unique
  }

  private def sqlRequestIdFromDeviceName_X_requests(table: Table, device: DeviceName): Stream[ConnectionIO, RecordId] = {
    (fr"SELECT id FROM " ++ Fragment.const(table.code + "_requests") ++ fr" WHERE device_name=$device")
      .query[RecordId].stream
  }

  private def sqlRequestIdFromDeviceLast_X_requests(table: Table, device: DeviceName): ConnectionIO[RecordId] = {
    (fr"SELECT MAX(id) FROM " ++ Fragment.const(table.code + "_requests") ++ fr" WHERE device_name=$device")
      .query[RecordId].unique
  }

  private def sqlActorTupsFromRequestId_X(table: Table, deviceId: RecordId): ConnectionIO[List[ActorTup]] = {
    (fr"SELECT request_id, device_name, actor_name, property_name, property_value, property_status FROM " ++ Fragment.const(table.code) ++ fr" WHERE request_id=$deviceId")
      .query[ActorTup].accumulate
  }

  private def sqlActorTupsFromPropId_X(table: Table, propId: RecordId): ConnectionIO[List[ActorTup]] = {
    (fr"SELECT request_id, device_name, actor_name, property_name, property_value, property_status FROM " ++ Fragment.const(table.code) ++ fr" WHERE id=$propId")
      .query[ActorTup].accumulate
  }

  private def sqlChangeStatusFromPropId_X(table: Table, propId: RecordId, newStatus: Status): ConnectionIO[Int] = {
    (fr"UPDATE " ++ Fragment.const(table.code) ++ fr" SET property_status = ${newStatus} WHERE id=$propId")
      .update.run
  }

  private def sqlPropIdsFromDeviceNameStatus_X(table: Table, device: DeviceName, status: Status): Stream[ConnectionIO, RecordId] = {
    (fr"SELECT request_id FROM " ++ Fragment.const(table.code) ++ fr" WHERE property_status=$status and device_name=$device")
      .query[RecordId].stream
  }

  private def sqlPropIdsFromDeviceNameActorNameStatus_X(table: Table, device: DeviceName, actor: ActorName, status: Status): Stream[ConnectionIO, RecordId] = {
    (fr"SELECT request_id FROM " ++ Fragment.const(table.code) ++ fr" WHERE property_status=$status and device_name=$device AND actor_name=$actor")
      .query[RecordId].stream
  }

  private def sqlPropIdsFromDeviceNameActorName_X(table: Table, device: DeviceName, actor: ActorName): Stream[ConnectionIO, RecordId] = {
    (fr"SELECT DISTINCT request_id FROM " ++ Fragment.const(table.code) ++ fr" WHERE device_name=$device AND actor_name =$actor")
      .query[RecordId].stream
  }

  private def sqlPropIdsFromDeviceName_X(table: Table, device: DeviceName): Stream[ConnectionIO, RecordId] = {
    (fr"SELECT DISTINCT request_id FROM " ++ Fragment.const(table.code) ++ fr" WHERE device_name=$device")
      .query[RecordId].stream
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