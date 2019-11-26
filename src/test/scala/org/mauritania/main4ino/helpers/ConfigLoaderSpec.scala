package org.mauritania.main4ino.helpers

import java.io.File

import cats.effect.IO
import com.typesafe.config.ConfigException
import org.mauritania.main4ino.{Config => GeneralConfig}
import org.mauritania.main4ino.Config.{DevLoggerConfig, FirmwareConfig, ServerConfig}
import org.mauritania.main4ino.db.Config.Cleanup
import org.mauritania.main4ino.db.{Config => DbConfig}
import org.mauritania.main4ino.security.{Config => SecurityConfig, Fixtures}
import org.scalatest._
import pureconfig._
import pureconfig.generic.auto._

class ConfigLoaderSpec extends FlatSpec with Matchers {

  "The config loader" should "load correctly a configuration file" in {
    val c = ConfigLoader.loadFromFile[IO, GeneralConfig](new File("src/test/resources/configs/1/application.conf")).unsafeRunSync()
    c shouldBe GeneralConfig(
      server = ServerConfig("0.0.0.0", 8080),
      database = DbConfig(
        driver = "org.h2.Driver",
        url = "jdbc:h2:mem:test-db",
        user = "sa",
        password = "",
        cleanup = Cleanup(
          periodSecs = 1,
          retentionSecs = 10
        )
      ),
      devLogger = DevLoggerConfig(
        logsBasePath = "/tmp"
      ),
      firmware = FirmwareConfig(
        firmwareBasePath = "src/test/resources/firmwares/1/"
      )
    )
  }

  val User1 = Fixtures.User1

  it should "load correctly a security configuration file" in {
    val c = ConfigLoader.loadFromFile[IO, SecurityConfig](new File("src/test/resources/configs/2/security-users-single.conf")).unsafeRunSync()
    c.users shouldBe List(User1)
  }

  it should "throw an exception if the config is invalid" in {
    a [IllegalArgumentException] should be thrownBy {
      ConfigLoader.loadFromFile[IO, SecurityConfig](new File("src/test/resources/configs/2/security-users-invalid.conf")).unsafeRunSync()
    }
  }

  it should "throw an exception if the config is malformed" in {
    a [ConfigException.Parse] should be thrownBy {
      ConfigLoader.loadFromFile[IO, SecurityConfig](new File("src/test/resources/configs/2/security-users-broken.conf")).unsafeRunSync()
    }
  }

}
