package org.mauritania.main4ino.db

import cats.effect.{Async, Blocker, IO, Resource, Sync}
import doobie.hikari.HikariTransactor
import eu.timepit.refined.types.numeric.PosInt
import org.mauritania.main4ino.Helper
import org.mauritania.main4ino.db.Config.Cleanup

trait TransactorCtx {

  val TransactorConfig = Config(
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    cleanup = Cleanup(
      periodSecs = PosInt(10),
      retentionSecs = PosInt(10)
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
