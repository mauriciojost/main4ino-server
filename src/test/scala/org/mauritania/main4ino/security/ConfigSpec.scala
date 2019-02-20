package org.mauritania.main4ino.security

import java.io.File

import com.typesafe.config.ConfigException
import org.scalatest._
import pureconfig.error.ConfigReaderException

class ConfigSpec extends FlatSpec with Matchers {

  val User1 = Fixtures.User1

  "The security config" should "load correctly a configuration file" in {
    val c = Config.load(new File("src/test/resources/security-users-single.conf")).unsafeRunSync()
    c.users shouldBe List(User1)
  }

  it should "throw an exception if the config is invalid" in {
    a [IllegalArgumentException] should be thrownBy {
      Config.load(new File("src/test/resources/security-users-invalid.conf")).unsafeRunSync()
    }
  }

  it should "throw an exception if the config is malformed" in {
    a [ConfigException.Parse] should be thrownBy {
      Config.load(new File("src/test/resources/security-users-broken.conf")).unsafeRunSync()
    }
  }

}
