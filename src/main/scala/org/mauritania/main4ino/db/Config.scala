package org.mauritania.main4ino.db

import eu.timepit.refined.types.numeric.PosInt
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
    periodSecs: PosInt,
    retentionSecs: PosInt
  )
}
