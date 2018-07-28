package org.mauritania.botinobe

import cats.effect.IO
import doobie.hikari.HikariTransactor
import org.mauritania.botinobe.db.{Config, Database}
import org.scalatest._

trait DbSuite extends FlatSpec with Matchers with BeforeAndAfterEach {

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
    Database.initialize(transactor, true).unsafeRunSync()
  }

  override def afterEach() = {
    transactor = null
  }

}
