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
      targetId <- sqlDeviceIn_report_requests(t)
      nroTargetActorProps <- sqlActorTupsIn_reports(t.asActorTups, targetId)
    } yield (targetId)
    transaction.transact(transactor)
  }

  def readReport(i: RecordId): IO[Device] = {
    val transaction = for {
      t <- sqlMetadataFromId_report_requests(i)
      p <- sqlActorTupsFromId_reports(i)
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }

  def readLastReport(device: DeviceName): IO[Device] = {
    val transaction = for {
      i <- sqlIdFromDeviceLast_report_requests(device)
      t <- sqlMetadataFromId_report_requests(i)
      p <- sqlActorTupsFromId_reports(i)
    } yield (Device.fromActorTups(t, p))
    transaction.transact(transactor)
  }



  private def sqlActorTupsIn_targets(t: Iterable[ActorTup], targetId: RecordId): ConnectionIO[Int] = {
    val sql = s"INSERT INTO targets (target_id, device_name, actor_name, property_name, property_value, property_status) VALUES (?, ?, ?, ?, ?, ?)"
    Update[ActorTup](sql).updateMany(t.toList.map(_.withId(Some(targetId))))
  }

  private def sqlActorTupsIn_reports(t: Iterable[ActorTup], targetId: RecordId): ConnectionIO[Int] = {
    val sql = s"INSERT into reports (target_id, device_name, actor_name, property_name, property_value, property_status) VALUES (?, ?, ?, ?, ?, ?)"
    Update[ActorTup](sql).updateMany(t.toList.map(_.withId(Some(targetId))))
  }


  private def sqlDeviceIn_target_requests(t: Device): ConnectionIO[RecordId] = {
    sql"INSERT INTO target_requests (creation, device_name) VALUES (${t.metadata.timestamp}, ${t.metadata.device})"
      .update.withUniqueGeneratedKeys[RecordId]("id")
  }

  private def sqlDeviceIn_report_requests(t: Device): ConnectionIO[RecordId] = {
    sql"INSERT INTO report_requests (creation, device_name) VALUES (${t.metadata.timestamp}, ${t.metadata.device})"
      .update.withUniqueGeneratedKeys[RecordId]("id")
  }

  private def sqlMetadataFromId_target_requests(id: RecordId): ConnectionIO[Metadata] = {
    sql"SELECT id, creation, device_name FROM target_requests WHERE id=$id"
      .query[Metadata].unique
  }

  private def sqlMetadataFromId_report_requests(id: RecordId): ConnectionIO[Metadata] = {
    sql"SELECT id, creation, device_nam FROM reports_requests WHERE id=$id"
      .query[Metadata].unique
  }

  private def sqlIdFromDeviceName_target_requests(device: DeviceName): Stream[ConnectionIO, RecordId] = {
    sql"SELECT id FROM target_requests WHERE device_name=$device"
      .query[RecordId].stream
  }

  private def sqlIdFromDeviceName_report_requests(device: DeviceName): Stream[ConnectionIO, RecordId] = {
    sql"SELECT id FROM report_requests WHERE device_name=$device"
      .query[RecordId].stream
  }

  private def sqlIdFromDeviceLast_target_requests(device: DeviceName): ConnectionIO[RecordId] = {
    sql"SELECT MAX(id) FROM target_requests WHERE device_name=$device"
      .query[RecordId].unique
  }

  private def sqlIdFromDeviceLast_report_requests(device: DeviceName): ConnectionIO[RecordId] = {
    sql"SELECT MAX(id) FROM report_requests WHERE device_name=$device"
      .query[RecordId].unique
  }

  private def sqlActorTupsFromId_targets(targetId: RecordId): ConnectionIO[List[ActorTup]] = {
    sql"SELECT target_id, device_name, actor_name, property_name, property_value, property_status FROM targets WHERE target_id=$targetId"
      .query[ActorTup].accumulate
  }

  private def sqlActorTupsFromId_reports(targetId: RecordId): ConnectionIO[List[ActorTup]] = {
    sql"SELECT target_id, device_name, actor_name, property_name, property_value, property_status FROM reports WHERE target_id=$targetId"
      .query[ActorTup].accumulate
  }

  private def sqlChangeStatus_targets(targetId: RecordId): ConnectionIO[Int] = {
    sql"UPDATE targets SET property_status = ${Status.Consumed} WHERE target_id=$targetId".update.run
  }

  private def sqlIdFromDeviceNameStatus_targets(device: DeviceName, status: Status): Stream[ConnectionIO, RecordId] = {
    sql"SELECT id FROM targets WHERE property_status=$status and device_name=$device"
      .query[RecordId].stream
  }

  private def sqlIdFromDeviceName_targets(device: DeviceName): Stream[ConnectionIO, RecordId] = {
    sql"SELECT DISTINCT target_id FROM targets WHERE device_name=$device"
      .query[RecordId].stream
  }

}
