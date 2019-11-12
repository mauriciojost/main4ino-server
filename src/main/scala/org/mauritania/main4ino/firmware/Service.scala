package org.mauritania.main4ino.firmware

import cats.effect.{Sync}
import fs2.Stream
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.{HttpService, Request, Response}
import org.http4s.dsl.Http4sDsl
import org.mauritania.main4ino.api.Attempt
import org.mauritania.main4ino.api.v1.Url.{FirmVersionParam, Platf, Proj}
import cats.implicits._
import org.mauritania.main4ino.firmware.Store.FirmwareCoords
import org.mauritania.main4ino.ContentTypeAppJson
import io.circe.syntax._
import org.http4s.circe._
import io.circe.generic.auto._

class Service[F[_] : Sync](st: Store[F]) extends Http4sDsl[F] {

  val service = HttpService[F] {

    /**
      * GET /firmwares/<project>/<platform>/content?version=<version>
      *
      * Example: GET /firmwares/botino/esp8266/content?version=3.1.8
      *
      * Retrieve a firmware given a the version, the platform and the project it belongs to.
      *
      * Returns: OK (200) | NO_CONTENT (204)
      */
    case a@GET -> Root / "firmwares" / Proj(project) / Platf(platform) / "content" :? FirmVersionParam(version) => {
      val S = implicitly[Sync[F]]
      val attempt: F[Attempt[Stream[F, Byte]]] = for {
        logger <- Slf4jLogger.fromClass[F](getClass)
        fa <- st.getFirmware(FirmwareCoords(project, version, platform))
        _ <- fa match {
          case Right(_) => S.delay(())
          case Left(msg) => logger.warn(msg)
        }
      } yield fa

      attempt.flatMap {
        case Right(v) => Ok(v)
        case Left(_) => NoContent()
      }
    }

    /**
      * GET /firmwares/<project>/<platform>
      *
      * Example: GET /firmwares/botino
      *
      * Retrieve a list of firmware coordinates available for download.
      *
      * Returns: OK (200) | NO_CONTENT (204)
      */
    case a@GET -> Root / "firmwares" / Proj(project) => {
      for {
        logger <- Slf4jLogger.fromClass[F](getClass)
        fa <- st.listFirmwares(project)
        _ <- logger.debug(s"Listing firmwares: $fa")
        r <- Ok(fa.asJson, ContentTypeAppJson)
      } yield r
    }

  }

  private[firmware] def request(r: Request[F]): F[Response[F]] = service.orNotFound(r)

}

