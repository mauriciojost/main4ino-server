package org.mauritania.botinobe.db

import cats.effect.IO
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

object Database {
  def transactor(config: Config): IO[HikariTransactor[IO]] = {
    HikariTransactor.newHikariTransactor[IO](config.driver, config.url, config.user, config.password)
  }

  def initialize(transactor: HikariTransactor[IO], clean: Boolean = false): IO[Unit] = {
    transactor.configure { datasource =>
      IO {
        val flyWay = new Flyway()
        flyWay.setDataSource(datasource)
        if (clean) {
          flyWay.clean()
        }
        flyWay.migrate()
      }
    }
  }
}
