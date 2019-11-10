package org.mauritania.main4ino.config

import java.io.File

import cats.effect.IO
import org.mauritania.main4ino.config.Config.{DevLoggerConfig, FirmwareConfig, ServerConfig}
import org.mauritania.main4ino.db.{Config => DbConfig}

case class Config(
  server: ServerConfig,
  database: DbConfig,
  devLogger: DevLoggerConfig,
  firmware: FirmwareConfig
)

object Config {
  import pureconfig._
  import pureconfig.generic.auto._

  def load(configFile: File): IO[Config] = Loadable.loadFromFile[IO, Config](configFile)

  case class ServerConfig(
    host: String,
    port: Int
  )

  case class DevLoggerConfig(
    logsBasePath: String
  )

  case class FirmwareConfig(
    firmwareBasePath: String
  )


}
