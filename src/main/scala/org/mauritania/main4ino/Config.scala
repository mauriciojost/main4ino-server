package org.mauritania.main4ino

import org.mauritania.main4ino.Config.{DevLoggerConfig, FirmwareConfig, ServerConfig}
import org.mauritania.main4ino.db.{Config => DbConfig}

case class Config(
  server: ServerConfig,
  database: DbConfig,
  devLogger: DevLoggerConfig,
  firmware: FirmwareConfig
)

object Config {

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
