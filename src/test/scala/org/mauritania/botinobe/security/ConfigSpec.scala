package org.mauritania.botinobe.security

import com.typesafe.config.ConfigException
import org.scalatest._
import pureconfig.error.ConfigReaderException

class ConfigSpec extends FlatSpec with Matchers {

  "The security config" should "load correctly a configuration file" in {
    val c = Config.load("users-single.conf").unsafeRunSync()
    c.users shouldBe List(
      User(1L, "mjost", "mauriciojost@gmail.com", List("/"), "012345678901234567890123456789")
    )
  }

  it should "load correctly a configuration file with repeated token for several users (removing them)" in {
    val c = Config.load("users-duplicate.conf").unsafeRunSync()
    c.users shouldBe List(
      User(1L, "mjost", "mauriciojost@gmail.com", List("/"), "012345678901234567890123456789")
      // duplicates for pedro removed as they have both identical token
    )
  }

  it should "throw an exception if the config is invalid" in {
    a [ConfigReaderException[Config]] should be thrownBy {
      Config.load("users-invalid.conf").unsafeRunSync()
    }
  }

  it should "throw an exception if the config is malformed" in {
    a [ConfigException.Parse] should be thrownBy {
      Config.load("users-broken.conf").unsafeRunSync()
    }
  }

}
