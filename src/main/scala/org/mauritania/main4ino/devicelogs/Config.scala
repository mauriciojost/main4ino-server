package org.mauritania.main4ino.devicelogs

import java.nio.file.Path

import eu.timepit.refined.types.numeric.PosInt

case class Config(
  logsBasePath: Path,
  maxLengthLogs: PosInt = PosInt(524288) // 512 KB
)
