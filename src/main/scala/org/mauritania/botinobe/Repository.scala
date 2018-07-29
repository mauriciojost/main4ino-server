package org.mauritania.botinobe

import cats.implicits._
import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie._
import doobie.implicits._
import org.mauritania.botinobe.models.Target.Metadata
import org.mauritania.botinobe.models._
import fs2.Stream

// Naming regarding to CRUD
class Repository(transactor: Transactor[IO]) {

  def createTarget(t: Target): IO[RecordId] = {
    val taps = t.asProps

    val transaction = for {
      targetId <- sqlInsertTarget(t)
      nroTargetActorProps <- sqlInsertTargetActorProps(taps, targetId)
    } yield (targetId)
    transaction.transact(transactor)
  }

  def readTarget(i: RecordId): IO[Target] = {
    val transaction = for {
      t <- sqlReadOneTarget(i)
      p <- sqlReadPropsOfTarget(i)
    } yield (Target.fromProps(t, p))
    transaction.transact(transactor)
  }

  def readTargetConsume(i: RecordId): IO[Target] = {
    val transaction = for {
      t <- sqlReadOneTarget(i)
      c <- sqlUpdateTargetAsConsumed(i)
      p <- sqlReadPropsOfTarget(i)
    } yield (Target.fromProps(t, p))
    transaction.transact(transactor)
  }

  def readTargetIds(device: DeviceName): Stream[IO, RecordId] = {
    sqlReadTargetIds(device).transact(transactor)
  }

  def readTargetIdsWhereStatus(device: DeviceName, status: Status): Stream[IO, RecordId] = {
    sqlReadTargetIdsWhereStatus(device, status).transact(transactor)
  }


  // targets table
  private def sqlInsertTarget(t: Target): ConnectionIO[RecordId] = {
    sql"INSERT INTO targets (status, device_name, creation) VALUES (${Status.Created}, ${t.metadata.device}, ${t.metadata.timestamp} )"
      .update.withUniqueGeneratedKeys[RecordId]("id")
  }

  private def sqlReadOneTarget(id: RecordId): ConnectionIO[Metadata] = {
    sql"SELECT status, device_name, creation from targets where id=$id"
      .query[Metadata].unique
  }

  private def sqlReadTargetIds(device: DeviceName): Stream[ConnectionIO, RecordId] = {
    sql"SELECT id from targets where device_name=$device"
      .query[RecordId].stream
  }

  private def sqlReadTargetIdsWhereStatus(device: DeviceName, status: Status): Stream[ConnectionIO, RecordId] = {
    sql"SELECT id from targets where device_name=$device and status=$status"
      .query[RecordId].stream
  }

  private def sqlUpdateTargetAsConsumed(targetId: RecordId): ConnectionIO[Int] = {
    sql"update targets set status = ${Status.Consumed} where id=$targetId".update.run
  }


  // target_props table

  private def sqlInsertTargetActorProps(t: Iterable[Prop], targetId: RecordId): ConnectionIO[Int] = {
    val sql = s"insert into target_props (target_id, actor_name, property_name, property_value) values (?, ?, ?, ?)"
    Update[(RecordId, Prop)](sql).updateMany(t.toList.map(m => (targetId, m)))
  }

  private def sqlReadPropsOfTarget(targetId: RecordId): ConnectionIO[List[Prop]] = {
    sql"SELECT actor_name, property_name, property_value from target_props where target_id=$targetId"
      .query[Prop].accumulate
  }

}
