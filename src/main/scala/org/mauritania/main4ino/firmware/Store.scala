package org.mauritania.main4ino.firmware

import java.io.FileNotFoundException
import java.nio.file.Path

import cats.effect.IO
import org.mauritania.main4ino.models.{FirmwareId, ProjectName}
import fs2.Stream
import org.mauritania.main4ino.api.Attempt

trait Store[F[_]] {
  def getFirmware(project: ProjectName, firmwareId: FirmwareId): F[Attempt[Stream[F, Byte]]]
}

class StoreIO(basePath: Path) extends Store[IO] {

  final val ChunkSize = 2048

  def getFirmware(project: ProjectName, firmwareId: FirmwareId): IO[Attempt[Stream[IO, Byte]]] = {
    val filename = s"$firmwareId.bin"
    val file = basePath.resolve(project).resolve(filename)
    val resp: IO[Attempt[Stream[IO, Byte]]] = IO {
      if (file.toFile.canRead) {
        Right(fs2.io.file.readAll[IO](file, ChunkSize))
      } else {
        Left(s"Could not find firmware: $project/$filename")
      }
    }
    resp
  }
}
