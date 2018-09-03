package org.mauritania.botinobe

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import pureconfig.error.ConfigReaderException

package object config {
  case class ServerConfig(host: String ,port: Int)

  case class Config(server: ServerConfig, database: db.Config)

  object Config {
    import pureconfig._

    def load(configFile: String): IO[Config] = {
      IO {
        loadConfig[Config](ConfigFactory.load(configFile))
      }.flatMap {
        case Left(e) => IO.raiseError[Config](new ConfigReaderException[Config](e))
        case Right(config) => IO.pure(config)
      }
    }
  }
}
