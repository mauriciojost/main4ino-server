package org.mauritania.main4ino

import java.nio.file.Path

import eu.timepit.refined.types.numeric.PosInt
import org.mauritania.main4ino.Config.{FirmwareConfig, ServerConfig}
import org.mauritania.main4ino.db.{Config => DbConfig}
import org.mauritania.main4ino.devicelogs.{Config => DevLoggerConfig}

case class Config(
  server: ServerConfig,
  database: DbConfig,
  devLogger: DevLoggerConfig,
  firmware: FirmwareConfig
)

object Config {

  case class ServerConfig(
    host: String,
    port: PosInt
  )

  case class FirmwareConfig(
    firmwareBasePath: String
  )

}
