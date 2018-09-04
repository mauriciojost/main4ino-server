package org.mauritania.botinobe.security

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import pureconfig.error.ConfigReaderException

case class Config(
  users: List[User]
) {
  def deduplicateTokens: Config = {
    val byToken = users.groupBy(_.token)
    val withSingleUser = byToken.collect {
      case (token, users) if users.length == 1 => users.head
    }
    Config(withSingleUser.toList)
  }
}

object Config {
  import pureconfig._

  def load(configFile: String): IO[Config] = {
    IO {
      loadConfig[Config](ConfigFactory.load(configFile))
    }.flatMap {
      case Left(e) => IO.raiseError[Config](new ConfigReaderException[Config](e))
      case Right(config) => IO.pure(config.deduplicateTokens)
    }
  }
}

