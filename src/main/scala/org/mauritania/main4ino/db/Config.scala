package org.mauritania.main4ino.db

import java.util.concurrent.TimeUnit

import eu.timepit.refined.types.numeric.{PosFloat, PosInt}
import org.mauritania.main4ino.db.Config.{Cleanup, DbSyntax}

import scala.concurrent.duration.FiniteDuration

case class Config(
  driver: String,
  url: String,
  user: String,
  password: String,
  cleanup: Cleanup
) {
  val syntax = DbSyntax.resolve(driver)
}

object Config {
  case class Cleanup(
    periodSecs: PosFloat,
    retentionSecs: PosInt
  ) {
    def periodDuration: FiniteDuration =
      FiniteDuration((periodSecs.value * 1000).toLong, TimeUnit.MILLISECONDS)
  }

  object DbSyntax { // TODO use enumeratum here ?
    sealed abstract class DbSyntax(val driver: String)
    case object Postgres extends DbSyntax("org.postgresql.Driver")
    case object H2 extends DbSyntax("org.h2.Driver")
    case object Sqlite extends DbSyntax("org.sqlite.JDBC")
    val all: List[DbSyntax] = List(Postgres, H2, Sqlite)
    def resolve(s: String): Option[DbSyntax] = all.find(_.driver == s)
  }

}
