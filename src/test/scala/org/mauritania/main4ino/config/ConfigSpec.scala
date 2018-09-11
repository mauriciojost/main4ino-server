package org.mauritania.main4ino.config

import org.mauritania.main4ino.config.Config.ServerConfig
import org.scalatest._
import org.mauritania.main4ino.db.{Config => DbConfig}

class ConfigSpec extends FlatSpec with Matchers {

  "The app config" should "load correctly a configuration file" in {
    val c = Config.load("application-valid.conf").unsafeRunSync()
    c shouldBe Config(
      ServerConfig("0.0.0.0", 8080),
      DbConfig("org.h2.Driver", "jdbc:h2:./db", "sa", "")
    )
  }

}
