package org.mauritania.main4ino.firmware

import org.mauritania.main4ino.firmware.Store.ByCoordsVer
import org.mauritania.main4ino.models.{Feature, FirmwareVersion, Platform, ProjectName, VersionWish}

import scala.util.Try

case class Wish(
  project: ProjectName,
  versionWish: VersionWish,
  platform: Platform
) {

  def resolve(
    available: Seq[Coord]
  ): Option[Coord] = {

    def latestWithFilter(filter: Coord => Boolean) =
      Try(available.filter(filter).max(ByCoordsVer)).toOption
    def concreteWithVersion(v: FirmwareVersion) =
      available.find(i => i.version == v && i.noFeature).headOption

    versionWish match {
      // Latest available non-feature version for the given project / platform
      case Wish.LatestKeyword => latestWithFilter(_.noFeature)
      // Latest available & stable non-feature version for the given project / platform
      case Wish.LatestStableKeyword => latestWithFilter(i => i.isStable && i.noFeature)
      // Latest available feature version (provided) for the given project / platform
      case Wish.LatestFeatureRegex(wishFeat) => latestWithFilter(_.hasFeature(wishFeat))
      // Provided feature & version for the given project / platform
      case Wish.VersionFeatureRegex(wishVer, wishFeat) => latestWithFilter(i => i.version == wishVer && i.hasFeature(wishFeat))
      // Absolute version provided
      case v if available.map(_.version).contains(v) => concreteWithVersion(v)
      case _ => None
    }
  }
}

object Wish {
  final lazy val LatestKeyword = "LATEST"
  final lazy val LatestStableKeyword = "LATEST_STABLE"
  final lazy val LatestFeatureRegex = (LatestKeyword ++ """_(\w+)""").r
  final lazy val VersionFeatureRegex = """(.*)_(\w+)""".r
}

