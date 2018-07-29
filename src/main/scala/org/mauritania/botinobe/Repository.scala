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
      targetId <- sqlDeviceIn_X_requests(table, t)
      nroTargetActorProps <- sqlActorTupsIn_X(table, t.asActorTups, targetId)
    } yield (targetId)
    transaction.transact(transactor)
  }

  def readDevice(table: Table, i: RecordId): IO[Device] = {
    val transaction = for {
      t <- sqlMetadataFromId_X_requests(table, i)
      p <- sqlActorTupsFromId_X(table, i)
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }


  def readLastDevice(table: Table, device: DeviceName): IO[Device] = {
    val transaction = for {
      i <- sqlIdFromDeviceLast_X_requests(table, device)
      t <- sqlMetadataFromId_X_requests(table, i)
      p <- sqlActorTupsFromId_X(table, i)
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }

  def readDeviceConsume(table: Table, i: RecordId): IO[Device] = {
    val transaction = for {
      t <- sqlMetadataFromId_X_requests(table, i)
      c <- sqlChangeStatus_X(table, i)
      p <- sqlActorTupsFromId_X(table, i)
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }

  def readDeviceIds(table: Table, device: DeviceName): Stream[IO, RecordId] = {
    sqlIdFromDeviceName_X_requests(table, device).transact(transactor)
  }

  def readDevicePropIdsWhereDeviceStatus(table: Table, device: DeviceName, status: Option[Status]): Stream[IO, RecordId] = {
    status match {
      case Some(s) => sqlIdFromDeviceNameStatus_X(table, device, s).transact (transactor)
      case None => sqlIdFromDeviceName_X(table, device).transact (transactor)
    }
  }

  def readDevicePropIdsWhereDeviceActorStatus(table: Table, device: DeviceName, actor: ActorName, status: Option[Status]): Stream[IO, RecordId] = {
    status match {
      case Some(s) => sqlIdFromDeviceNameActorNameStatus_X(table: Table, device, actor, s).transact (transactor)
      case None => sqlIdFromDeviceNameActorName_X(table: Table, device, actor).transact (transactor)
    }
  }


  private def sqlActorTupsIn_X(table: Table, t: Iterable[ActorTup], targetId: RecordId): ConnectionIO[Int] = {
    val sql = s"INSERT INTO ${table.code} (target_id, device_name, actor_name, property_name, property_value, property_status) VALUES (?, ?, ?, ?, ?, ?)"
    Update[ActorTup](sql).updateMany(t.toList.map(_.withId(Some(targetId))))
  }

  private def sqlDeviceIn_X_requests(table: Table, t: Device): ConnectionIO[RecordId] = {
    (fr"INSERT INTO " ++ Fragment.const(table.code + "_requests") ++ fr" (creation, device_name) VALUES (${t.metadata.timestamp}, ${t.metadata.device})")
      .update.withUniqueGeneratedKeys[RecordId]("id")
  }

  private def sqlMetadataFromId_X_requests(table: Table, id: RecordId): ConnectionIO[Metadata] = {
    (fr"SELECT id, creation, device_name FROM " ++ Fragment.const(table.code + "_requests") ++ fr" WHERE id=$id")
      .query[Metadata].unique
  }

  private def sqlIdFromDeviceName_X_requests(table: Table, device: DeviceName): Stream[ConnectionIO, RecordId] = {
    (fr"SELECT id FROM " ++ Fragment.const(table.code + "_requests") ++ fr" WHERE device_name=$device")
      .query[RecordId].stream
  }

  private def sqlIdFromDeviceLast_X_requests(table: Table, device: DeviceName): ConnectionIO[RecordId] = {
    (fr"SELECT MAX(id) FROM " ++ Fragment.const(table.code + "_requests") ++ fr" WHERE device_name=$device")
      .query[RecordId].unique
  }

  private def sqlActorTupsFromId_X(table: Table, targetId: RecordId): ConnectionIO[List[ActorTup]] = {
    (fr"SELECT target_id, device_name, actor_name, property_name, property_value, property_status FROM " ++ Fragment.const(table.code) ++ fr" WHERE target_id=$targetId")
      .query[ActorTup].accumulate
  }

  private def sqlChangeStatus_X(table: Table, targetId: RecordId): ConnectionIO[Int] = {
    (fr"UPDATE " ++ Fragment.const(table.code) ++ fr" SET property_status = ${Status.Consumed} WHERE target_id=$targetId")
      .update.run
  }

  private def sqlIdFromDeviceNameStatus_X(table: Table, device: DeviceName, status: Status): Stream[ConnectionIO, RecordId] = {
    (fr"SELECT target_id FROM " ++ Fragment.const(table.code) ++ fr" WHERE property_status=$status and device_name=$device")
      .query[RecordId].stream
  }

  private def sqlIdFromDeviceNameActorNameStatus_X(table: Table, device: DeviceName, actor: ActorName, status: Status): Stream[ConnectionIO, RecordId] = {
    (fr"SELECT target_id FROM " ++ Fragment.const(table.code) ++ fr" WHERE property_status=$status and device_name=$device AND actorn_name=$actor")
      .query[RecordId].stream
  }

  private def sqlIdFromDeviceNameActorName_X(table: Table, device: DeviceName, actor: ActorName): Stream[ConnectionIO, RecordId] = {
    (fr"SELECT DISTINCT target_id FROM " ++ Fragment.const(table.code) ++ fr" WHERE device_name=$device AND actor_name =$actor")
      .query[RecordId].stream
  }

  private def sqlIdFromDeviceName_X(table: Table, device: DeviceName): Stream[ConnectionIO, RecordId] = {
    (fr"SELECT DISTINCT target_id FROM " ++ Fragment.const(table.code) ++ fr" WHERE device_name=$device")
      .query[RecordId].stream
  }

}

object Repository {

  object Table {

    sealed abstract class Table(val code: String)
    case object Reports extends Table("targets")
    case object Targets extends Table("reports")

    val all = List(Reports, Targets)
    def resolve(s: String): Option[Table] = all.find(_.code == s)
  }

}