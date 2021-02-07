package org.mauritania.main4ino.firmware

import java.io.File
import com.gilt.gfc.semver.SemVer
import org.mauritania.main4ino.firmware.Coord.FirmwareFile
import org.mauritania.main4ino.models.{Feature, FirmwareFilename, FirmwareVersion, Platform, ProjectName}
import enumeratum._

case class Coord(
  project: ProjectName,
  version: FirmwareVersion,
  platform: Platform,
  filename: FirmwareFilename,
  feature: Option[Feature] = None
) {
  def semVer: SemVer = SemVer(version)
  def isStable: Boolean = semVer.extra.isEmpty
  def hasFeature(f: Feature): Boolean = feature.contains(f)
  def noFeature: Boolean = feature.isEmpty
  def resolve(kind: FirmwareFile): String = {
    kind match {
      case FirmwareFile.Bin => filename
      case FirmwareFile.Elf => filename.replaceAll(Coord.BinExtensionRegex, ".elf")
      case FirmwareFile.JsonDesc => filename.replaceAll(Coord.BinExtensionRegex, ".description.json")
    }
  }
}

object Coord {
  final val CoordsRegularRegex = """firmware-(.*)\.(\w+).bin""".r
  final val CoordsWithFeatureRegex = """firmware-(.*)_(\w+).(\w+).bin""".r
  final val BinExtensionRegex = "\\.bin$"

  sealed abstract class FirmwareFile(override val entryName: String) extends EnumEntry
  object FirmwareFile extends Enum[FirmwareFile] {
    val values = findValues
    case object Elf extends FirmwareFile("elf")
    case object Bin extends FirmwareFile("bin")
    case object JsonDesc extends FirmwareFile("description")
  }

  def fromFile(f: File): Option[Coord] = {
    val project = f.getParentFile.getName
    val fname = f.getName
    fname match {
      case CoordsWithFeatureRegex(version, feature, platform) => Some(Coord(project, version, platform, fname, Some(feature)))
      case CoordsRegularRegex(version, platform) => Some(Coord(project, version, platform, fname, None))
      case _ => None
    }
  }
}

