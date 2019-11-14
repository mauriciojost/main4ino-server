package org.mauritania.main4ino.config

import java.io.File

import org.mauritania.main4ino.config.Config.{DevLoggerConfig, FirmwareConfig, ServerConfig}
import org.mauritania.main4ino.db.Config.Cleanup
import org.scalatest._
import org.mauritania.main4ino.db.{Config => DbConfig}

class ConfigSpec extends FlatSpec with Matchers {

  "The app config" should "load correctly a configuration file" in {
    val c = Config.load(new File("src/test/resources/configs/3/application-valid.conf")).unsafeRunSync()
    c shouldBe Config(
      server = ServerConfig("0.0.0.0", 8080),
      database = DbConfig(
        driver = "org.h2.Driver",
        url = "jdbc:h2:./db",
        user = "sa",
        password = "",
        cleanup = Cleanup(
          periodSecs = 86400,
          retentionSecs = 604800
        )
      ),
      devLogger = DevLoggerConfig(
        logsBasePath = "/tmp"
      ),
      firmware = FirmwareConfig(
        firmwareBasePath = "/tmp"
      )
    )
  }

}
