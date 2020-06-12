package org.mauritania.main4ino.devicelogs

import java.nio.file.Path

import eu.timepit.refined.types.numeric.PosInt
import org.mauritania.main4ino.devicelogs.Partitioner.{IdentityPartitioner, Partitioner}

case class Config(
  logsBasePath: Path,
  maxLengthLogs: PosInt = Config.DefaultMaxLengthLogs,
  partitioner: Partitioner = IdentityPartitioner
)

object Config {
  final lazy val DefaultMaxLengthLogs = PosInt(524288) // 512 KB
}
