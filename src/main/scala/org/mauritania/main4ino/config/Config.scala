package org.mauritania.main4ino.config

import cats.effect.IO
import org.mauritania.main4ino.config.Config.ServerConfig
import org.mauritania.main4ino.db.{Config => DbConfig}

case class Config(server: ServerConfig, database: DbConfig)

object Config extends Loadable[Config] {

  def load(configFile: String): IO[Config] = load(configFile, identity)
  case class ServerConfig(host: String ,port: Int)

}
