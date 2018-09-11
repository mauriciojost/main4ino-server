package org.mauritania.main4ino.config

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import org.mauritania.main4ino.config.Config.ServerConfig
import pureconfig.error.ConfigReaderException
import org.mauritania.main4ino.db.{Config => DbConfig}

case class Config(server: ServerConfig, database: DbConfig)

object Config {
  import pureconfig._

  case class ServerConfig(host: String ,port: Int)

  def load(configFile: String): IO[Config] = {
    IO {
      loadConfig[Config](ConfigFactory.load(configFile))
    }.flatMap {
      case Left(e) => IO.raiseError[Config](new ConfigReaderException[Config](e))
      case Right(config) => IO.pure(config)
    }
  }
}
