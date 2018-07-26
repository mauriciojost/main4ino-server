package org.mauritania.botinobe

import java.sql.Timestamp
import java.util.{Calendar, UUID}

import cats._
import cats.implicits._
import cats.effect.IO
import doobie.util.transactor.Transactor
import fs2.Stream
import doobie._
import doobie.implicits._
import org.mauritania.botinobe.Models._

class Repository(transactor: Transactor[IO]) {

  case class TargetRecord(id: RecordId, time: Timestamp, actor: String, property: String, value: String)

  private def withId(taps: List[TargetActorProp], tId: RecordId): List[TargetActorProp] = taps.map(_.copy(targetId = tId))

  /*
  def toIO[K](value: Stream[IO, K]): IO[K] = {

  }
  */

  def createTarget(t: Target): IO[RecordId] = {
    val taps = t.expand.toList

    val a = for {
      tId <- sql"INSERT INTO targets (status, device) VALUES ('created', ${t.device})".update.withUniqueGeneratedKeys[RecordId]("id")
      //tapIds <- toIO(Update[TargetActorProp] ("INSERT INTO target_props (target_id, actor, property, value) VALUES (?, ?, ?, ?)")
      //  .updateManyWithGeneratedKeys[RecordId] ("id") (withId(taps, tId)))
    } yield (tId)
    //} yield ((tId, tapIds))

    a.transact(transactor)

  }

}
