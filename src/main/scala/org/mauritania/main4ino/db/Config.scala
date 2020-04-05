package org.mauritania.main4ino.db

import java.util.concurrent.TimeUnit

import eu.timepit.refined.types.numeric.{PosFloat, PosInt}
import org.mauritania.main4ino.db.Config.Cleanup

import scala.concurrent.duration.FiniteDuration

case class Config(
  driver: String,
  url: String,
  user: String,
  password: String,
  cleanup: Cleanup
)

object Config {
  case class Cleanup(
    periodSecs: PosFloat,
    retentionSecs: PosInt
  ) {
    def periodDuration: FiniteDuration =
      FiniteDuration((periodSecs.value * 1000).toLong, TimeUnit.MILLISECONDS)
  }
}
