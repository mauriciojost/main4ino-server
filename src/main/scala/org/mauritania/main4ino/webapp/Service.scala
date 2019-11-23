package org.mauritania.main4ino.webapp

import cats.implicits._
import cats.effect.{Blocker, IO}
import org.http4s.{HttpService, Request, Response, StaticFile}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.staticcontent
import org.http4s.server.staticcontent.ResourceService.Config
import org.mauritania.main4ino.helpers.HttpMeter

import scala.concurrent.ExecutionContext

class Service(resourceIndexHtml: String, ec: ExecutionContext, blocker: Blocker) extends Http4sDsl[IO] {

  private implicit val cs = IO.contextShift(ec)
  private val StaticResource = staticcontent.resourceService[IO](
    Config(
      basePath = "/webapp",
      pathPrefix = "/",
      cacheStrategy = staticcontent.MemoryCache(),
      blocker = blocker
    )
  )

  val serviceUntimed = HttpService[IO] {

      case a@GET -> Root =>
        StaticFile.fromResource(resourceIndexHtml, blocker, Some(a)).getOrElseF(InternalServerError())

      case a@GET -> _ =>
        StaticResource(a).value.map(_.getOrElse(Response.notFound[IO]))

    }

  val service = HttpMeter.timedHttpMiddleware[IO].apply(serviceUntimed)

	private[webapp] def request(r: Request[IO]): IO[Response[IO]] = service(r).getOrElseF(NotFound())

}

