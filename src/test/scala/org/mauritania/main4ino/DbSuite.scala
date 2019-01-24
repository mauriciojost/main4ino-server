package org.mauritania.main4ino

import cats.effect.IO
import doobie.hikari.HikariTransactor
import org.mauritania.main4ino.db.Config.Cleanup
import org.mauritania.main4ino.db.{Config, Database}
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
      t <- Database.transactor(transactorConfig)
      d <- Database.initialize(t, true)
    } yield(t)
    transactor = k.unsafeRunSync
  }

  override def afterEach() = {
    transactor = null
  }

}
