package org.mauritania.main4ino.db

import cats.effect.{Async, Blocker, ContextShift, Resource, Sync}
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway
import cats.implicits._
import cats.effect.implicits._

import scala.concurrent.ExecutionContext

object Database {
  def transactor[F[_]: Sync: ContextShift: Async](config: Config, ec: ExecutionContext): Resource[F, HikariTransactor[F]] = {
    HikariTransactor
      .newHikariTransactor[F](config.driver, config.url, config.user, config.password, ec, Blocker.liftExecutionContext(ec))
  }

  def initialize[F[_]: Sync](transactor: HikariTransactor[F], clean: Boolean = false): F[Unit] = {
    transactor.configure { datasource =>
      Sync[F].delay {
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
