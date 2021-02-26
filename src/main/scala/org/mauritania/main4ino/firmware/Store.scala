package org.mauritania.main4ino.firmware

import java.io.File
import java.nio.file.Path
import cats.effect.Sync
import com.gilt.gfc.semver.SemVer
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.mauritania.main4ino.api.Attempt
import org.mauritania.main4ino.models.{Platform, ProjectName}
import cats.implicits._
import org.mauritania.main4ino.firmware.Coord.FirmwareFile

object Store {

  final val ByCoordsVer = Ordering.by[Coord, SemVer](i => SemVer(i.version))

}

class Store[F[_]: Sync](basePath: Path) {

  private def length(f: File): F[Long] = Sync[F].delay(f.length)
  private def isReadableFile(f: File): F[Boolean] = Sync[F].delay(f.canRead && f.isFile)
  private def listFiles(dir: File): F[List[File]] =
    Sync[F].delay(Option(dir.listFiles()).toList.flatMap(_.toList))

  def getFirmware(wish: Wish): F[Attempt[Coord]] = {
    val resp: F[Attempt[Coord]] = for {
      logger <- Slf4jLogger.fromClass[F](getClass)
      coords <- listFirmwares(wish.project, wish.platform)
      coord = wish.resolve(coords)
      _ <- logger.debug(s"Wished firmware: $wish, resolved: $coord")
      result = coord.toRight(s"Could not resolve: $wish")
    } yield result
    resp
  }

  def getFirmwareFile(coord: Coord, resource: FirmwareFile = FirmwareFile.Bin): F[Attempt[File]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](getClass)
      projPath = basePath.resolve(coord.project)
      file = projPath.resolve(coord.resolve(resource))
      checked <- checkFile(file.toFile)
      _ <- logger.debug(s"Requested firmware file: $coord/$resource -> $checked")
    } yield checked
  }

  def listFirmwares(project: ProjectName, platform: Platform): F[Seq[Coord]] = {
    val path = basePath.resolve(project)
    listFiles(path.toFile).map { files =>
      files.flatMap(Coord.fromFile).filter(_.platform == platform).sorted(Store.ByCoordsVer)
    }
  }

  private def checkFile(f: File): F[Attempt[File]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](getClass)
      readable <- isReadableFile(f)
      length <- length(f)
      _ <- logger.debug(s"Sanity checked firmware file $f: readable=$readable length=$length")
      located = readable match {
        case true => Right(f)
        case false =>
          Left(s"Could not locate/read $f")
      }
    } yield located
  }

}
