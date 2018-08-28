package org.mauritania.botinobe.security

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import pureconfig.error.ConfigReaderException

case class Config(users: List[User])

object Config {
  import pureconfig._

  def load(configFile: String = "users.conf"): IO[Config] = {
    IO {
      loadConfig[Config](ConfigFactory.load(configFile))
    }.flatMap {
      case Left(e) => IO.raiseError[Config](new ConfigReaderException[Config](e))
      case Right(config) => IO.pure(config)
    }
  }
}

// TESTS for config and user
