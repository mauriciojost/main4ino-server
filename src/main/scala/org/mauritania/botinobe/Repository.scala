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
      targetId <- sqlDeviceIn_target_requests(t)
      nroTargetActorProps <- sqlActorTupsIn_targets(t.asActorTups, targetId)
    } yield (targetId)
    transaction.transact(transactor)
  }

  def readTarget(i: RecordId): IO[Device] = {
    val transaction = for {
      t <- sqlMetadataFromId_target_requests(i)
      p <- sqlActorTupsFromId_targets(i)
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }


  def readLastTarget(device: DeviceName): IO[Device] = {
    val transaction = for {
      i <- sqlIdFromDeviceLast_target_requests(device)
      t <- sqlMetadataFromId_target_requests(i)
      p <- sqlActorTupsFromId_targets(i)
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }

  def readTargetConsume(i: RecordId): IO[Device] = {
    val transaction = for {
      t <- sqlMetadataFromId_target_requests(i)
      c <- sqlChangeStatus_targets(i)
      p <- sqlActorTupsFromId_targets(i)
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }

  def readTargetIds(device: DeviceName): Stream[IO, RecordId] = {
    sqlIdFromDeviceName_target_requests(device).transact(transactor)
  }

  def readTargetPropIdsWhereStatus(device: DeviceName, status: Option[Status]): Stream[IO, RecordId] = {
    status match {
      case Some(s) => sqlIdFromDeviceNameStatus_targets(device, s).transact (transactor)
      case None => sqlIdFromDeviceName_targets(device).transact (transactor)
    }
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


  // target_requests table

  private def sqlDeviceIn_target_requests(t: Device): ConnectionIO[RecordId] = {
    sql"INSERT INTO target_requests (creation, device_name) VALUES (${t.metadata.timestamp}, ${t.metadata.device})"
      .update.withUniqueGeneratedKeys[RecordId]("id")
  }

  private def sqlMetadataFromId_target_requests(id: RecordId): ConnectionIO[Metadata] = {
    sql"SELECT id, creation, device_name FROM target_requests WHERE id=$id"
      .query[Metadata].unique
  }

  private def sqlIdFromDeviceName_target_requests(device: DeviceName): Stream[ConnectionIO, RecordId] = {
    sql"SELECT id FROM target_requests WHERE device_name=$device"
      .query[RecordId].stream
  }

  private def sqlIdFromDeviceNameStatus_targets(device: DeviceName, status: Status): Stream[ConnectionIO, RecordId] = {
    sql"SELECT id FROM targets WHERE property_status=$status and device_name=$device"
      .query[RecordId].stream
  }

  private def sqlIdFromDeviceName_targets(device: DeviceName): Stream[ConnectionIO, RecordId] = {
    sql"SELECT DISTINCT target_id FROM targets WHERE device_name=$device"
      .query[RecordId].stream
  }


  private def sqlIdFromDeviceLast_target_requests(device: DeviceName): ConnectionIO[RecordId] = {
    sql"SELECT MAX(id) FROM target_requests WHERE device_name=$device"
      .query[RecordId].unique
  }


  // targets table

  private def sqlActorTupsIn_targets(t: Iterable[ActorTup], targetId: RecordId): ConnectionIO[Int] = {
    val sql = s"INSERT INTO targets (target_id, device_name, actor_name, property_name, property_value, property_status) VALUES (?, ?, ?, ?, ?, ?)"
    Update[ActorTup](sql).updateMany(t.toList.map(_.withId(Some(targetId))))
  }

  private def sqlActorTupsFromId_targets(targetId: RecordId): ConnectionIO[List[ActorTup]] = {
    sql"SELECT target_id, device_name, actor_name, property_name, property_value, property_status FROM targets WHERE target_id=$targetId"
      .query[ActorTup].accumulate
  }

  private def sqlChangeStatus_targets(targetId: RecordId): ConnectionIO[Int] = {
    sql"UPDATE targets SET property_status = ${Status.Consumed} WHERE target_id=$targetId".update.run
  }


  // reports table

  private def sqlDeviceIn_reports(t: Device): ConnectionIO[RecordId] = {
    sql"INSERT INTO reports (property_status, device_name, creation) VALUES (${Status.Created}, ${t.metadata.device}, ${t.metadata.timestamp} )"
      .update.withUniqueGeneratedKeys[RecordId]("id")
  }

  private def sqlMetadataFromId_reports(id: RecordId): ConnectionIO[Metadata] = {
    sql"SELECT id, property_status, device_name, creation FROM reports WHERE id=$id"
      .query[Metadata].unique
  }

  private def sqlIdFromDeviceLast_reports(device: DeviceName): ConnectionIO[RecordId] = {
    sql"SELECT MAX(id) FROM reports WHERE device_name=$device"
      .query[RecordId].unique
  }

  private def sqlIdFromDeviceName_reports(device: DeviceName): Stream[ConnectionIO, RecordId] = {
    sql"SELECT id FROM reports WHERE device_name=$device"
      .query[RecordId].stream
  }


  // report_props table

  private def sqlActorTupsIn_report_props(t: Iterable[ActorTup], targetId: RecordId): ConnectionIO[Int] = {
    val sql = s"INSERT into report_props (target_id, actor_name, property_name, property_value, property_status) VALUES (?, ?, ?, ?, ?)"
    Update[(RecordId, ActorTup)](sql).updateMany(t.toList.map(m => (targetId, m)))
  }

  private def sqlActorTupsFromId_report_props(targetId: RecordId): ConnectionIO[List[ActorTup]] = {
    sql"SELECT actor_name, property_name, property_value, property_status FROM report_props WHERE target_id=$targetId"
      .query[ActorTup].accumulate
  }


}
