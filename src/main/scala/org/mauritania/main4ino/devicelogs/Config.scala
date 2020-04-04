package org.mauritania.main4ino.devicelogs

import java.nio.file.Path

import eu.timepit.refined.types.numeric.PosInt

case class Config(
  logsBasePath: Path,
  maxLengthLogs: PosInt = Config.DefaultMaxLengthLogs
)

object Config {
  final lazy val DefaultMaxLengthLogs = PosInt(512 * 1024)// 512 KB
}
