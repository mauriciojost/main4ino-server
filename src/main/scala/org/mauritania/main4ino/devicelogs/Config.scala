package org.mauritania.main4ino.devicelogs

import java.nio.file.Path

import eu.timepit.refined.types.numeric.PosInt

case class Config(
  logsBasePath: Path,
  maxLengthLogs: PosInt = PosInt(1024 * 512) // 512 KB
)

