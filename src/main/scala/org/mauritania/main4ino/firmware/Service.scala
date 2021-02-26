package org.mauritania.main4ino.firmware

import java.io.File
import cats.effect.{Blocker, ContextShift, Effect, Sync}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.http4s.{EntityEncoder, Header, Headers, HttpRoutes, Request, Response, StaticFile}
import org.http4s.headers.`Content-Length`
import org.http4s.dsl.Http4sDsl
import org.mauritania.main4ino.api.v1.Url.{FirmFileModeParam, Platf, Proj, VerWishParam}
import cats.implicits._
import org.mauritania.main4ino.ContentTypeAppJson
import io.circe.syntax._
import org.http4s.circe._
import io.circe.generic.auto._
import org.http4s.util.CaseInsensitiveString
import org.mauritania.main4ino.models.FirmwareVersion
import cats._
import cats.data._
import io.circe.Encoder
import org.mauritania.main4ino.firmware.Coord.FirmwareFile
import org.mauritania.main4ino.helpers.HttpMeter

import scala.concurrent.ExecutionContext

class Service[F[_]: Sync: Effect: ContextShift](store: Store[F], ec: ExecutionContext)
    extends Http4sDsl[F] {

  import Service._

  final private val blocker = Blocker.liftExecutionContext(ec)

  val serviceUntimed = HttpRoutes.of[F] {

    /**
      * GET /firmwares/<project>/<platform>/content?version=<version>
      *
      * Example: GET /firmwares/botino/esp8266/content?version=3.1.8
      * Example: GET /firmwares/botino/esp8266/content?version=3.1.8&mode=elf
      * Example: GET /firmwares/botino/esp8266/content?version=3.1.8&mode=description
      *
      * Retrieve a firmware given a the version, the platform and the project it belongs to.
      *
      * Returns: OK (200) | NO_CONTENT (204)
      */
    case a @ GET -> Root / "firmwares" / Proj(project) / Platf(platform) / "content" :? VerWishParam(versionFeatureCode) +& FirmFileModeParam(fileType) => {
      val currVers = extractCurrentVersion(a.headers)
      val wish = Wish(project, versionFeatureCode, platform)

      for {
        logger <- Slf4jLogger.fromClass[F](getClass)
        _ <- logger.debug(s"Requested firmware: wish=$wish / device=$currVers / headers=${a.headers} / filetype=$fileType")
        coordinate <- store.getFirmware(wish)
        response <- coordinate match {
          case Right(coord) if (currVers.exists(_ == coord.version)) => // same version as current
              logger.debug(s"Firmware already up-to-date: $currVers=${coord.version}...").flatMap(_ => NotModified())
          case Right(coord) => // different version than current, serving...
            for {
              file <- store.getFirmwareFile(coord, fileType.getOrElse(FirmwareFile.Bin))
              _ <- logger.info(s"Firmware NOT up-to-date, proposing $coord / $file...")
              fileServed <- file match {
                case Right(f) => StaticFile.fromFile(f, blocker, Some(a)).getOrElseF(InternalServerError())
                case Left(e) => logger.warn(s"No such file: $e").flatMap(_ => NotFound())
              }
            } yield fileServed
          case Left(msg) => // no such version
            logger.warn(s"Cannot propose firmware, version not found: $msg").flatMap(_ => NotFound())
        }
      } yield response
    }

    /**
      * GET /firmwares/<project>/<platform>/metadata?version=<version>
      *
      * Example: GET /firmwares/botino/esp8266/metadata?version=3.1.8
      *
      * Retrieve a the metadata of firmware given a the version, the platform and the project it belongs to.
      *
      * Returns: OK (200) | NO_CONTENT (204)
      */
    case GET -> Root / "firmwares" / Proj(project) / Platf(platform) / "metadata" :? VerWishParam(
          versionFeatureCode
        ) => {
      val coords = Wish(project, versionFeatureCode, platform)
      for {
        logger <- Slf4jLogger.fromClass[F](getClass)
        _ <- logger.debug(s"Requested firmware metadata: $coords")
        fa <- store.getFirmware(coords)
        r <- fa match {
          case Right(x) => Ok(x.asJson, ContentTypeAppJson)
          case Left(_) => NoContent()
        }
      } yield r
    }

    /**
      * GET /firmwares/<project>/<platform>
      *
      * Example: GET /firmwares/botino/esp8266
      *
      * Retrieve a list of firmware coordinates available for download.
      *
      * Returns: OK (200)
      */
    case a @ GET -> Root / "firmwares" / Proj(project) / Platf(platform) => {
      for {
        logger <- Slf4jLogger.fromClass[F](getClass)
        fa <- store.listFirmwares(project, platform)
        _ <- logger.debug(s"Listing firmwares for $project/$platform: ${fa.size} found")
        r <- Ok(fa.asJson, ContentTypeAppJson)
      } yield r
    }

  }

  private[firmware] def extractCurrentVersion(h: Headers): Option[FirmwareVersion] = {
    val currentVersions: List[Header] = VersionHeaders.flatMap(i => h.get(i).toList)
    currentVersions match {
      case Nil => None // unknown firmware version in requester
      case one :: Nil =>
        Some(one.value) // a single (as expected) header reported the firmware version in requester
      case _ =>
        None // multiple (unexpected) headers reported the firmware version in requester (config problem?)
    }
  }

  val service = HttpMeter.timedHttpMiddleware[F].apply(serviceUntimed)

  private[firmware] def request(r: Request[F]): F[Response[F]] = service(r).getOrElseF(NotFound())

}

object Service {

  // TODO make this all configurable
  /**
    * Headers that are used by different platforms to report the current firmware version they are running.
    * This allows the device to report the current version, and let the server tell if such version is the most
    * up to date or not.
    */
  val Esp8266VersionHeader = "x-ESP8266-version"
  val Esp32VersionHeader = "x-ESP32-version"
  val VersionHeaders: List[CaseInsensitiveString] = List(
    Esp8266VersionHeader,
    Esp32VersionHeader
  ).map(CaseInsensitiveString.apply)

  implicit val CirceFileEncoder: Encoder[File] = Encoder.encodeString.contramap[File](_.getName)

}
