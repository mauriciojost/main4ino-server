package org.mauritania.botinobe

import java.sql.Timestamp
import java.util.{Calendar, UUID}

import cats._
import cats.implicits._
import cats.effect.IO
import cats.free.Free
import doobie.util.transactor.Transactor
import fs2.Stream
import doobie._
import doobie.free.connection.ConnectionOp
import doobie.implicits._
import org.mauritania.botinobe.Models._

class Repository(transactor: Transactor[IO]) {

  def createTarget(t: Target): IO[RecordId] = {
    val taps = t.expand.toList

    val transaction = for {
      targetId <- insertOneTarget(t)
      nroTargetActorProps <- insertManyTargetActorProps(taps, targetId)
    } yield (targetId)

    transaction.transact(transactor)

  }

  private def insertOneTarget(t: Target): ConnectionIO[RecordId] = {
    sql"INSERT INTO targets (status, device_name) VALUES ('created', ${t.device})".update.withUniqueGeneratedKeys[RecordId]("id")
  }

  private def insertManyTargetActorProps(t: List[TargetActorProp], targetId: RecordId): ConnectionIO[Int] = {
    val sql = s"insert into target_props (target_id, actor_name, property_name, property_value) values ($targetId, ?, ?, ?)"
    Update[TargetActorProp](sql).updateMany(t)
  }


}
