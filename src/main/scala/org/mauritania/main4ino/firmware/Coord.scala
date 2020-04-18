package org.mauritania.main4ino.firmware

import java.io.File

import com.gilt.gfc.semver.SemVer
import org.mauritania.main4ino.models.{Feature, FirmwareFilename, FirmwareVersion, Platform, ProjectName}

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
}

object Coord {
  final val CoordsRegularRegex = """firmware-(.*)\.(\w+).bin""".r
  final val CoordsWithFeatureRegex = """firmware-(.*)_(\w+).(\w+).bin""".r

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

