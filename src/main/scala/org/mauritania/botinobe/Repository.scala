package org.mauritania.botinobe

import cats.implicits._
import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie._
import doobie.implicits._
import org.mauritania.botinobe.models.Target.Metadata
import org.mauritania.botinobe.models.{DeviceName, Prop, RecordId, Target}
import fs2.Stream

// Naming regarding to CRUD
class Repository(transactor: Transactor[IO]) {

  def createTarget(t: Target): IO[RecordId] = {
    val taps = t.expandProps.toList

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
    } yield (Target.fromListOfProps(t, p))
    transaction.transact(transactor)
  }

  def readAndUpdateTargetAsConsumed(i: RecordId): IO[Target] = {
    val transaction = for {
      t <- sqlReadOneTarget(i)
      c <- sqlUpdateTargetAsConsumed(i)
      p <- sqlReadPropsOfTarget(i)
    } yield (Target.fromListOfProps(t, p))
    transaction.transact(transactor)
  }

  def readTargetIds(device: DeviceName): Stream[IO, RecordId] = {
    sqlReadTargetIds(device).transact(transactor)
  }

  private def sqlInsertTarget(t: Target): ConnectionIO[RecordId] = {
    sql"INSERT INTO targets (status, device_name) VALUES (${Target.Created}, ${t.metadata.device})"
      .update.withUniqueGeneratedKeys[RecordId]("id")
  }

  private def sqlInsertTargetActorProps(t: List[Prop], targetId: RecordId): ConnectionIO[Int] = {
    val sql = s"insert into target_props (target_id, actor_name, property_name, property_value) values (?, ?, ?, ?)"
    Update[(RecordId, Prop)](sql).updateMany(t.map(m => (targetId, m)))
  }

  private def sqlReadOneTarget(id: RecordId): ConnectionIO[Metadata] = {
    sql"SELECT status, device_name from targets where id=$id"
      .query[Metadata].unique
  }

  private def sqlReadTargetIds(device: DeviceName): Stream[ConnectionIO, RecordId] = {
    sql"SELECT id from targets where device_name=$device"
      .query[RecordId].stream
  }

  private def sqlReadPropsOfTarget(targetId: RecordId): ConnectionIO[List[Prop]] = {
    sql"SELECT actor_name, property_name, property_value from target_props where target_id=$targetId"
      .query[Prop].accumulate
  }

  private def sqlUpdateTargetAsConsumed(targetId: RecordId): ConnectionIO[Int] = {
    sql"update targets set status = ${Target.Consumed} where id=$targetId".update.run
  }

}
