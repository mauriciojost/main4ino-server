package org.mauritania.botinobe

import cats.implicits._
import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie._
import doobie.implicits._
import org.mauritania.botinobe.models.Device.Metadata
import org.mauritania.botinobe.models._
import fs2.Stream

// Naming regarding to CRUD
class Repository(transactor: Transactor[IO]) {


  // Targets

  def createTarget(t: Device): IO[RecordId] = {
    val transaction = for {
      targetId <- sqlDeviceIn_targets(t)
      nroTargetActorProps <- sqlActorTupsIn_target_props(t.asActorTups, targetId)
    } yield (targetId)
    transaction.transact(transactor)
  }

  def readTarget(i: RecordId): IO[Device] = {
    val transaction = for {
      t <- sqlMetadataFromId_targets(i)
      p <- sqlActorTupsFromId_target_props(i)
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }


  def readLastTarget(device: DeviceName): IO[Device] = {
    val transaction = for {
      i <- sqlIdFromDeviceLast_targets(device)
      t <- sqlMetadataFromId_targets(i)
      p <- sqlActorTupsFromId_target_props(i)
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }

  def readTargetConsume(i: RecordId): IO[Device] = {
    val transaction = for {
      t <- sqlMetadataFromId_targets(i)
      c <- sqlChangeStatus_targets(i)
      p <- sqlActorTupsFromId_target_props(i)
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }

  def readTargetIds(device: DeviceName): Stream[IO, RecordId] = {
    sqlIdFromDeviceName_targets(device).transact(transactor)
  }

  def readTargetIdsWhereStatus(device: DeviceName, status: Status): Stream[IO, RecordId] = {
    sqlIdFromDeviceStatus_targets(device, status).transact(transactor)
  }

  // Reports

  def createReport(t: Device): IO[RecordId] = {
    val transaction = for {
      targetId <- sqlDeviceIn_reports(t)
      nroTargetActorProps <- sqlActorTupsIn_report_props(t.asActorTups, targetId)
    } yield (targetId)
    transaction.transact(transactor)
  }

  def readReport(i: RecordId): IO[Device] = {
    val transaction = for {
      t <- sqlMetadataFromId_reports(i)
      p <- sqlActorTupsFromId_report_props(i)
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }

  def readLastReport(device: DeviceName): IO[Device] = {
    val transaction = for {
      i <- sqlIdFromDeviceLast_reports(device)
      t <- sqlMetadataFromId_reports(i)
      p <- sqlActorTupsFromId_report_props(i)
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }


  // targets table

  private def sqlDeviceIn_targets(t: Device): ConnectionIO[RecordId] = {
    sql"INSERT INTO targets (status, device_name, creation) VALUES (${Status.Created}, ${t.metadata.device}, ${t.metadata.timestamp} )"
      .update.withUniqueGeneratedKeys[RecordId]("id")
  }

  private def sqlMetadataFromId_targets(id: RecordId): ConnectionIO[Metadata] = {
    sql"SELECT id, status, device_name, creation from targets where id=$id"
      .query[Metadata].unique
  }

  private def sqlIdFromDeviceName_targets(device: DeviceName): Stream[ConnectionIO, RecordId] = {
    sql"SELECT id from targets where device_name=$device"
      .query[RecordId].stream
  }

  private def sqlIdFromDeviceStatus_targets(device: DeviceName, status: Status): Stream[ConnectionIO, RecordId] = {
    sql"SELECT id from targets where device_name=$device and status=$status"
      .query[RecordId].stream
  }

  private def sqlChangeStatus_targets(targetId: RecordId): ConnectionIO[Int] = {
    sql"update targets set status = ${Status.Consumed} where id=$targetId".update.run
  }

  private def sqlIdFromDeviceLast_targets(device: DeviceName): ConnectionIO[RecordId] = {
    sql"SELECT MAX(id) from targets where device_name=$device"
      .query[RecordId].unique
  }


  // target_props table

  private def sqlActorTupsIn_target_props(t: Iterable[ActorTup], targetId: RecordId): ConnectionIO[Int] = {
    val sql = s"insert into target_props (target_id, actor_name, property_name, property_value) values (?, ?, ?, ?)"
    Update[(RecordId, ActorTup)](sql).updateMany(t.toList.map(m => (targetId, m)))
  }

  private def sqlActorTupsFromId_target_props(targetId: RecordId): ConnectionIO[List[ActorTup]] = {
    sql"SELECT actor_name, property_name, property_value from target_props where target_id=$targetId"
      .query[ActorTup].accumulate
  }

  // reports table

  private def sqlDeviceIn_reports(t: Device): ConnectionIO[RecordId] = {
    sql"INSERT INTO reports (status, device_name, creation) VALUES (${Status.Created}, ${t.metadata.device}, ${t.metadata.timestamp} )"
      .update.withUniqueGeneratedKeys[RecordId]("id")
  }

  private def sqlMetadataFromId_reports(id: RecordId): ConnectionIO[Metadata] = {
    sql"SELECT id, status, device_name, creation from reports where id=$id"
      .query[Metadata].unique
  }

  private def sqlIdFromDeviceLast_reports(device: DeviceName): ConnectionIO[RecordId] = {
    sql"SELECT MAX(id) from reports where device_name=$device"
      .query[RecordId].unique
  }

  private def sqlIdFromDeviceName_reports(device: DeviceName): Stream[ConnectionIO, RecordId] = {
    sql"SELECT id from reports where device_name=$device"
      .query[RecordId].stream
  }

  private def sqlIdFromDeviceStatus_reports(device: DeviceName, status: Status): Stream[ConnectionIO, RecordId] = {
    sql"SELECT id from reports where device_name=$device and status=$status"
      .query[RecordId].stream
  }

  private def sqlChangeStatus_reports(targetId: RecordId): ConnectionIO[Int] = {
    sql"update reports set status = ${Status.Consumed} where id=$targetId".update.run
  }


  // report_props table

  private def sqlActorTupsIn_report_props(t: Iterable[ActorTup], targetId: RecordId): ConnectionIO[Int] = {
    val sql = s"insert into report_props (target_id, actor_name, property_name, property_value) values (?, ?, ?, ?)"
    Update[(RecordId, ActorTup)](sql).updateMany(t.toList.map(m => (targetId, m)))
  }

  private def sqlActorTupsFromId_report_props(targetId: RecordId): ConnectionIO[List[ActorTup]] = {
    sql"SELECT actor_name, property_name, property_value from report_props where target_id=$targetId"
      .query[ActorTup].accumulate
  }


}
