package org.mauritania.main4ino.helpers

import java.io.File
import java.nio.file.Paths

import cats.effect.IO
import com.typesafe.config.ConfigException
import org.mauritania.main4ino.{Config => GeneralConfig}
import org.mauritania.main4ino.Config.{FirmwareConfig, ServerConfig}
import org.mauritania.main4ino.db.Config.Cleanup
import org.mauritania.main4ino.db.{Config => DbConfig}
import org.mauritania.main4ino.devicelogs.Config
import org.mauritania.main4ino.security.{Fixtures, Config => SecurityConfig}
import org.scalatest._
import pureconfig._
import pureconfig.generic.auto._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import eu.timepit.refined.pureconfig._
import eu.timepit.refined.types.numeric.PosInt
import pureconfig.error.ConfigReaderException

class ConfigLoaderSpec extends AnyFlatSpec with Matchers {

  "The config loader" should "load correctly a configuration file" in {
    val c = ConfigLoader.loadFromFile[IO, GeneralConfig](new File("src/test/resources/configs/1/application.conf")).unsafeRunSync()
    c shouldBe GeneralConfig(
      server = ServerConfig("0.0.0.0", PosInt(8080)),
      database = DbConfig(
        driver = "org.h2.Driver",
        url = "jdbc:h2:mem:test-db",
        user = "sa",
        password = "",
        cleanup = Cleanup(
          periodSecs = PosInt(1),
          retentionSecs = PosInt(10)
        )
      ),
      devLogger = Config(Paths.get("/tmp")),
      firmware = FirmwareConfig(
        firmwareBasePath = "src/test/resources/firmwares/1/"
      )
    )
  }

  val User1 = Fixtures.User1

  import ConfigLoader.PureConfigImplicits._

  it should "load correctly a security configuration file" in {
    val c = ConfigLoader.loadFromFile[IO, SecurityConfig](new File("src/test/resources/configs/2/security-users-single.conf")).unsafeRunSync()
    c.users shouldBe List(User1)
  }

  it should "throw an exception if the config is invalid" in {
    a [ConfigReaderException[SecurityConfig]] should be thrownBy {
      ConfigLoader.loadFromFile[IO, SecurityConfig](new File("src/test/resources/configs/2/security-users-invalid.conf")).unsafeRunSync()
    }
  }

  it should "throw an exception if the config is malformed" in {
    a [ConfigReaderException[SecurityConfig]] should be thrownBy {
      ConfigLoader.loadFromFile[IO, SecurityConfig](new File("src/test/resources/configs/2/security-users-broken.conf")).unsafeRunSync()
    }
  }

}
