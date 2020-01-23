package org.mauritania.main4ino.logs

import java.nio.file.Path

case class DevLoggerConfig(
  logsBasePath: Path,
  maxLengthLogs: Int = 1024 * 512 // 512 KB
)

