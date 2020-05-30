package org.mauritania.main4ino.devicelogs

import java.nio.file.Path

import eu.timepit.refined.types.numeric.{NonNegInt, PosInt}

case class Config(
  logsBasePath: Path,
  maxLengthLogs: PosInt = Config.DefaultMaxLengthLogs,
  partitionPos: NonNegInt = Config.DefaultPartitionPos
)

object Config {
  final lazy val DefaultMaxLengthLogs = PosInt(524288) // 512 KB
  final lazy val DefaultPartitionPos = NonNegInt(5) // 100000 secs (around 27 hours)
}
