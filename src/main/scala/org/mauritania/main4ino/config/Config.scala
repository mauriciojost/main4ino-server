package org.mauritania.main4ino.config

import java.io.File

import cats.effect.IO
import org.mauritania.main4ino.config.Config.ServerConfig
import org.mauritania.main4ino.db.{Config => DbConfig}

case class Config(server: ServerConfig, database: DbConfig)

object Config extends Loadable {

  def load(configFile: File): IO[Config] = loadFromFile[Config](configFile)
  case class ServerConfig(host: String ,port: Int)

}
