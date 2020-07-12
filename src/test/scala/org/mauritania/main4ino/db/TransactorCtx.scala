package org.mauritania.main4ino.db

import java.util.UUID

import cats.effect.{Async, Blocker, IO, Resource, Sync}
import doobie.hikari.HikariTransactor
import eu.timepit.refined.types.numeric.{PosFloat, PosInt}
import org.mauritania.main4ino.Helper
import org.mauritania.main4ino.db.Config.Cleanup

trait TransactorCtx {

  val TransactorConfig = Config(
    driver = "org.h2.Driver",
    url = buildUrl("default"),
    user = "sa",
    password = "",
    cleanup = Cleanup(
      periodSecs = PosFloat(10),
      retentionSecs = PosInt(10)
    )
  )

  private def buildUrl(name: String) = s"jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1"

  private def randomName() = "test-" + UUID.randomUUID.toString

  def withTransactorWithName[T](name: String)(f: HikariTransactor[IO] => T): T = {
    val ec = Helper.testExecutionContext
    val re = for {
      t <- Database.transactor[IO](TransactorConfig.copy(url = buildUrl(name)), ec, Blocker.liftExecutionContext(ec))(Sync[IO], IO.contextShift(ec), Async[IO])
      _ <- Resource.liftF(Database.initialize[IO](t, true))
    } yield(t)
    re.use(c => IO(f(c))).unsafeRunSync()
  }

  def withTransactor[T](f: HikariTransactor[IO] => T): T = {
    val name = randomName()
    withTransactorWithName(name)(f)
  }

}
