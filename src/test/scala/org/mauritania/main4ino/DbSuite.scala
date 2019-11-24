package org.mauritania.main4ino

import cats.effect.{Async, Blocker, IO, Resource, Sync}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.mauritania.main4ino.db.Config.Cleanup
import org.mauritania.main4ino.db.{Config, Database}
import org.mauritania.main4ino.helpers.{Time, TimeIO}
import org.scalatest._

trait DbSuite extends FlatSpec with Matchers with BeforeAndAfterEach {

  var transactor: HikariTransactor[IO] = null

  val transactorConfig = Config(
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    cleanup = Cleanup(
      periodSecs = 10,
      retentionSecs = 10
    )
  )


  override def beforeEach() = {
    val k = for {
      ec <- ExecutionContexts.cachedThreadPool[IO]
      t <- Database.transactor[IO](transactorConfig, ec)(Sync[IO], IO.contextShift(ec), Async[IO])
      d <- Resource.liftF(Database.initialize[IO](t, true))
    } yield(t)
    transactor = k.use(_ => IO.never).unsafeRunSync()
  }

  override def afterEach() = {
    transactor = null
  }

}
