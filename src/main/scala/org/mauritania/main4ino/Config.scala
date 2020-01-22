package org.mauritania.main4ino

import java.nio.file.Path

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
    logsBasePath: Path,
    maxLengthLogs: Int = 1024 * 512 // 512 KB
  )

  case class FirmwareConfig(
    firmwareBasePath: String
  )

}
