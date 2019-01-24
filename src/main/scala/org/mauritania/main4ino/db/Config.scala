package org.mauritania.main4ino.db

import org.mauritania.main4ino.db.Config.Cleanup

case class Config(
  driver: String,
  url: String,
  user: String,
  password: String,
  cleanup: Cleanup
)

object Config {
  case class Cleanup(
    periodSecs: Int,
    retentionSecs: Int
  )
}
