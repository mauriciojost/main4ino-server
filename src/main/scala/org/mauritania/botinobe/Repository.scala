package org.mauritania.botinobe

import cats.implicits._
import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie._
import doobie.implicits._
import org.mauritania.botinobe.models.TargetStatus._
import org.mauritania.botinobe.models.Target.Metadata
import org.mauritania.botinobe.models.{Prop, RecordId, Target}

// Naming regarding to CRUD
class Repository(transactor: Transactor[IO]) {

  def createTarget(t: Target): IO[RecordId] = {
    val taps = t.expandProps.toList

    val transaction = for {
      targetId <- insertOneTarget(t)
      nroTargetActorProps <- insertManyTargetActorProps(taps, targetId)
    } yield (targetId)

    transaction.transact(transactor)

  }

  def readTarget(i: RecordId): IO[Target] = {
    val transaction = for {
      t <- readOneTarget1(i)
      p <- readProps1OfOneTarget(i)
    } yield (Target.fromListOfProps(t, p))

    transaction.transact(transactor)

  }

  private def insertOneTarget(t: Target): ConnectionIO[RecordId] = {
    sql"INSERT INTO targets (status, device_name) VALUES (${Target.Created}, ${t.metadata.device})"
      .update.withUniqueGeneratedKeys[RecordId]("id")
  }

  private def insertManyTargetActorProps(t: List[Prop], targetId: RecordId): ConnectionIO[Int] = {
    val sql = s"insert into target_props (target_id, actor_name, property_name, property_value) values (?, ?, ?, ?)"
    Update[(RecordId, Prop)](sql).updateMany(t.map(m => (targetId, m)))
  }

  private def readOneTarget1(id: RecordId): ConnectionIO[Metadata] = {
    sql"SELECT status, device_name from targets where id=$id"
      .query[Metadata].unique
  }

  private def readProps1OfOneTarget(targetId: RecordId): ConnectionIO[List[Prop]] = {
    sql"SELECT actor_name, property_name, property_value from target_props where target_id=$targetId"
      .query[Prop].accumulate
  }

}
