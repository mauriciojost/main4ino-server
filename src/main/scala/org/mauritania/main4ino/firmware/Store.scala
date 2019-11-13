package org.mauritania.main4ino.firmware

import java.io.File
import java.nio.file.Path

import cats.effect.IO
import org.mauritania.main4ino.api.Attempt
import org.mauritania.main4ino.firmware.Store.{Firmware, FirmwareCoords}
import org.mauritania.main4ino.models.{FirmwareVersion, Platform, ProjectName}

trait Store[F[_]] {
  def getFirmware(coords: FirmwareCoords): F[Attempt[Firmware]]
  def listFirmwares(project: ProjectName): F[Set[FirmwareCoords]]
}

object Store {

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
  private def canRead(f: File): IO[Boolean] = IO(f.canRead)
  private def listFiles(base: File): IO[Array[File]] = IO(base.listFiles())

  override def getFirmware(coords: FirmwareCoords): IO[Attempt[Firmware]] = {
    val filename = s"firmware-${coords.version}.${coords.platform}.bin"
    val file = basePath.resolve(coords.project).resolve(filename).toFile
    val resp: IO[Attempt[Firmware]] = for {
      readable <- canRead(file)
      length <- length(file)
      r = readable match {
        case true => Right(Firmware(file, length))
        case false => Left(s"Could not locate/read firmware: ${coords.project}/$filename (resolved to $file)")
      }
    } yield r
    resp
  }

  override def listFirmwares(project: ProjectName): IO[Set[FirmwareCoords]] = {
    val path = basePath.resolve(project)
    listFiles(path.toFile).map { files =>
      files.flatMap(FirmwareCoords.fromFile).toSet
    }
  }

}
