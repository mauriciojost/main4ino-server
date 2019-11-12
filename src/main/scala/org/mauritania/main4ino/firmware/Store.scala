package org.mauritania.main4ino.firmware

import java.io.File
import java.nio.file.Path

import cats.effect.IO
import fs2.Stream
import org.mauritania.main4ino.api.Attempt
import org.mauritania.main4ino.firmware.Store.FirmwareCoords
import org.mauritania.main4ino.models.{FirmwareVersion, Platform, ProjectName}

trait Store[F[_]] {
  def getFirmware(coords: FirmwareCoords): F[Attempt[Stream[F, Byte]]]

  def listFirmwares(project: ProjectName): F[Set[FirmwareCoords]]
}

object Store {

  case class FirmwareCoords(
    project: ProjectName,
    version: FirmwareVersion,
    platform: Platform
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

  final val ChunkSize = 2048

  override def getFirmware(coords: FirmwareCoords): IO[Attempt[Stream[IO, Byte]]] = {
    val filename = s"firmware-${coords.version}.${coords.platform}.bin"
    val file = basePath.resolve(coords.project).resolve(filename)
    val resp: IO[Attempt[Stream[IO, Byte]]] = IO {
      if (file.toFile.canRead) {
        Right(fs2.io.file.readAll[IO](file, ChunkSize))
      } else {
        Left(s"Could not locate/read firmware: ${coords.project}/$filename (resolved to $file)")
      }
    }
    resp
  }

  override def listFirmwares(project: ProjectName): IO[Set[FirmwareCoords]] = IO {
    val path = basePath.resolve(project)
    val files = path.toFile.listFiles()
    val coords = files.flatMap(FirmwareCoords.fromFile).toSet
    coords
  }

}
