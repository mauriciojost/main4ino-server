package org.mauritania.main4ino.firmware

import java.io.File
import java.nio.file.Path

import cats.effect.Sync
import com.gilt.gfc.semver.SemVer
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.mauritania.main4ino.api.Attempt
import org.mauritania.main4ino.firmware.Store.{Firmware, FirmwareCoords}
import org.mauritania.main4ino.models.{FirmwareVersion, Platform, ProjectName}
import cats.implicits._

object Store {

  final val BySemVer = Ordering.ordered[SemVer]

  case class FirmwareCoords(
    project: ProjectName,
    version: FirmwareVersion,
    platform: Platform
  )

  case class Firmware(
    file: File,
    length: Long,
    coords: FirmwareCoords
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

class Store[F[_]: Sync](basePath: Path) {

  private def length(f: File): F[Long] = Sync[F].delay(f.length)
  private def isReadableFile(f: File): F[Boolean] = Sync[F].delay(f.canRead && f.isFile)
  private def listFiles(dir: File): F[List[File]] = Sync[F].delay(Option(dir.listFiles()).toList.flatMap(_.toList))

  def getFirmware(coords: FirmwareCoords): F[Attempt[Firmware]] = {
    val resp: F[Attempt[Firmware]] = for {
      logger <- Slf4jLogger.fromClass[F](getClass)
      _ <- logger.debug(s"Retrieving firmware: $coords")
      available <- listFirmwares(coords.project, coords.platform)
      resolved = resolveVersion(coords, available)
      _ <- logger.debug(s"Resolved firmware: $coords")
      checked <- resolved match {
        case Some(v) => checkVersion(v)
        case None => Sync[F].delay[Attempt[Firmware]](Left(s"Could not resolve: $coords"))
      }
    } yield checked
    resp
  }

  def listFirmwares(project: ProjectName, platform: Platform): F[Set[FirmwareCoords]] = {
    val path = basePath.resolve(project)
    listFiles(path.toFile).map { files =>
      files.flatMap(FirmwareCoords.fromFile).filter(_.platform == platform).toSet
    }
  }

  private def checkVersion(coords: FirmwareCoords): F[Attempt[Firmware]] = {
    val filename = s"firmware-${coords.version}.${coords.platform}.bin"
    val file = basePath.resolve(coords.project).resolve(filename).toFile
    for {
      logger <- Slf4jLogger.fromClass[F](getClass)
      readable <- isReadableFile(file)
      length <- length(file)
      _ <- logger.debug(s"Checked firmware $coords: readable=$readable length=$length")
      located = readable match {
        case true => Right(Firmware(file, length, coords))
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
