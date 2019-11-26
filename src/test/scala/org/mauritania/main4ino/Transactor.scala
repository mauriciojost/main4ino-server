package org.mauritania.main4ino

import cats.effect.{Async, Blocker, IO, Resource, Sync}
import doobie.hikari.HikariTransactor
import org.mauritania.main4ino.db.Config.Cleanup
import org.mauritania.main4ino.db.{Config, Database}

trait Transactor {

  val TransactorConfig = Config(
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    cleanup = Cleanup(
      periodSecs = 10,
      retentionSecs = 10
    )
  )

  def withTransactor[T](f: HikariTransactor[IO] => T): T = {
    val ec = Helper.testExecutionContext
    val cs = IO.contextShift(ec)
    val re = for {
      t <- Database.transactor[IO](TransactorConfig, ec, Blocker.liftExecutionContext(ec))(Sync[IO], IO.contextShift(ec), Async[IO])
      d <- Resource.liftF(Database.initialize[IO](t, true))
    } yield(t)
    re.use(c => IO(f(c))).unsafeRunSync()
  }

}
