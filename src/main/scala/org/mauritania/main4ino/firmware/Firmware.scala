package org.mauritania.main4ino.firmware

import java.io.File

case class Firmware(
  file: File,
  length: Long,
  coords: Coord
) {
  def elfFile: File =  file.toPath.getParent.resolve(file.getName + ".elf").toFile
}

