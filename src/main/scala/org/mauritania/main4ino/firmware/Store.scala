package org.mauritania.main4ino.firmware

import java.io.File
import java.nio.file.Path

import cats.effect.Sync
import com.gilt.gfc.semver.SemVer
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.mauritania.main4ino.api.Attempt
import org.mauritania.main4ino.models.{Platform, ProjectName}
import cats.implicits._

object Store {

  final val ByCoordsVer = Ordering.by[Coord, SemVer](i => SemVer(i.version))

}

class Store[F[_]: Sync](basePath: Path) {

  private def length(f: File): F[Long] = Sync[F].delay(f.length)
  private def isReadableFile(f: File): F[Boolean] = Sync[F].delay(f.canRead && f.isFile)
  private def listFiles(dir: File): F[List[File]] =
    Sync[F].delay(Option(dir.listFiles()).toList.flatMap(_.toList))

  def getFirmware(coords: Wish): F[Attempt[Firmware]] = {
    val resp: F[Attempt[Firmware]] = for {
      logger <- Slf4jLogger.fromClass[F](getClass)
      available <- listFirmwares(coords.project, coords.platform)
      resolved = coords.resolve(available)
      _ <- logger.debug(s"Requested firmware: $coords, resolved: $resolved")
      checked <- resolved match {
        case Some(v) => checkCoordsFile(v)
        case None => Sync[F].delay[Attempt[Firmware]](Left(s"Could not resolve: $coords"))
      }
    } yield checked
    resp
  }

  def listFirmwares(project: ProjectName, platform: Platform): F[Seq[Coord]] = {
    val path = basePath.resolve(project)
    listFiles(path.toFile).map { files =>
      files.flatMap(Coord.fromFile).filter(_.platform == platform).sorted(Store.ByCoordsVer)
    }
  }

  private def checkCoordsFile(coords: Coord): F[Attempt[Firmware]] = {
    val file = basePath.resolve(coords.project).resolve(coords.filename).toFile
    for {
      logger <- Slf4jLogger.fromClass[F](getClass)
      readable <- isReadableFile(file)
      length <- length(file)
      _ <- logger.debug(s"Sanity checked firmware file $file/$coords: readable=$readable length=$length")
      located = readable match {
        case true => Right(Firmware(file, length, coords))
        case false =>
          Left(s"Could not locate/read firmware: ${coords.project}/${coords.filename} (resolved to $file)")
      }
    } yield located
  }

}
