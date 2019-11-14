package org.mauritania.main4ino.firmware

import java.io.File
import java.nio.file.Path

import cats.effect.IO
import com.gilt.gfc.semver.SemVer
import org.mauritania.main4ino.api.Attempt
import org.mauritania.main4ino.firmware.Store.{Firmware, FirmwareCoords}
import org.mauritania.main4ino.models.{FirmwareVersion, Platform, ProjectName}

trait Store[F[_]] {
  def getFirmware(coords: FirmwareCoords): F[Attempt[Firmware]]
  def listFirmwares(project: ProjectName, platform: Platform): F[Set[FirmwareCoords]]
}

object Store {

  final val BySemVer = Ordering.ordered[SemVer]

  case class FirmwareCoords(
    project: ProjectName,
    version: FirmwareVersion,
    platform: Platform
  )

  case class Firmware(
    file: File,
    length: Long
  )

  object FirmwareCoords {
    final val FileNameRegex = """firmware-(.*)\.(\w+).bin""".r

    def fromFile(f: File): Option[FirmwareCoords] = {
      val project = f.getParentFile.getName
      val fname = f.getName
      fname match {
        case FileNameRegex(version, platform) => Some(FirmwareCoords(project, version, platform))
        case _ => None
      }
    }
  }

}

class StoreIO(basePath: Path) extends Store[IO] {

  private def length(f: File): IO[Long] = IO(f.length)
  private def isReadableFile(f: File): IO[Boolean] = IO(f.canRead && f.isFile)
  private def listFiles(dir: File): IO[List[File]] = IO(Option(dir.listFiles()).toList.flatMap(_.toList))

  override def getFirmware(coords: FirmwareCoords): IO[Attempt[Firmware]] = {
    val resp: IO[Attempt[Firmware]] = for {
      available <- listFirmwares(coords.project, coords.platform)
      resolved = resolveVersion(coords, available)
      checked <- resolved match {
        case Some(v) => checkVersion(v)
        case None => IO.pure[Attempt[Firmware]](Left(s"Could not resolve: $coords"))
      }
    } yield checked
    resp
  }

  override def listFirmwares(project: ProjectName, platform: Platform): IO[Set[FirmwareCoords]] = {
    val path = basePath.resolve(project)
    listFiles(path.toFile).map { files =>
      files.flatMap(FirmwareCoords.fromFile).filter(_.platform == platform).toSet
    }
  }

  private def checkVersion(coords: FirmwareCoords): IO[Attempt[Firmware]] = {
    val filename = s"firmware-${coords.version}.${coords.platform}.bin"
    val file = basePath.resolve(coords.project).resolve(filename).toFile
    for {
      readable <- isReadableFile(file)
      length <- length(file)
      located = readable match {
        case true => Right(Firmware(file, length))
        case false => Left(s"Could not locate/read firmware: ${coords.project}/$filename (resolved to $file)")
      }
    } yield located
  }

  private def resolveVersion(target: FirmwareCoords, available: Set[FirmwareCoords]): Option[FirmwareCoords] = {
    import Store._
    target match {
      case _ if available.isEmpty =>
        None
      case c if (c.version == "LATEST") =>
        val semvers = available.map(c => SemVer.apply(c.version))
        Some(
          FirmwareCoords(
            project = target.project,
            version = semvers.max(BySemVer).original,
            platform = target.platform
          )
        )
      case t if available.contains(t) =>
        Some(t)
      case _ =>
        None
    }
  }


}
