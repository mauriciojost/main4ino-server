package org.mauritania.main4ino.security

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import pureconfig.error.ConfigReaderException

case class Config(
  users: List[User]
) {
  def deduplicateTokens: Config = {
    val byToken = users.groupBy(_.token)
    val withSingleUser = byToken.collect {
      case (_, users) if users.length == 1 => users.head
    }
    Config(withSingleUser.toList)
  }
}

// TODO this can be abstracted (and de-duplicated): see repeated config.Config
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

