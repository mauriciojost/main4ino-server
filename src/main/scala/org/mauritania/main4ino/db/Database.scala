package org.mauritania.main4ino.db

import cats.effect.{Blocker, IO, Resource}
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway
import cats.implicits._
import cats.effect.implicits._

import scala.concurrent.ExecutionContext

object Database {
  def transactor(config: Config, ec: ExecutionContext, blocker: Blocker): Resource[IO, HikariTransactor[IO]] = {
    implicit val cs = IO.contextShift(ec) // TODO review this, not clear
    HikariTransactor.newHikariTransactor[IO](config.driver, config.url, config.user, config.password, ec, blocker)
  }

  def initialize(transactor: HikariTransactor[IO], clean: Boolean = false): IO[Unit] = {
    transactor.configure { datasource =>
      IO {
        val flyWay = Flyway
          .configure()
          .dataSource(datasource)
          .load()
        if (clean) {
          flyWay.clean()
        }
        flyWay.migrate()
      }
    }
  }
}
