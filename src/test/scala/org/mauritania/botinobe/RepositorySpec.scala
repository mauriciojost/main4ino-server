package org.mauritania.botinobe.models

import cats.effect.IO
import doobie._
import doobie.hikari.HikariTransactor
import doobie.scalatest.IOChecker
import org.flywaydb.core.Flyway
import org.mauritania.botinobe.{Fixtures, Repository}
import org.mauritania.botinobe.Fixtures.TargetFixture1
import org.mauritania.botinobe.db.{Config, Database}
import org.scalatest._

class RepositorySpec extends FunSuite with Matchers with BeforeAndAfterEach {

  var transactor: HikariTransactor[IO] = null

  val transactorConfig = Config(
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = ""
  )

  override def beforeEach() = {
    val db = Database.transactor(transactorConfig)
    transactor = db.unsafeRunSync()
    Database.initialize(transactor).unsafeRunSync()
  }

  override def afterEach() = {
    transactor = null
  }


  test("") {
    val repo = new Repository(transactor)
    repo.createTarget(TargetFixture1).unsafeRunSync() shouldBe(1L)
    val target = repo.readTarget(1L)
    target.unsafeRunSync() shouldBe(TargetFixture1)
  }

}
