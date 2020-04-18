package org.mauritania.main4ino.firmware

import java.io.File

case class Firmware(
  file: File,
  length: Long,
  coords: Coord
)

