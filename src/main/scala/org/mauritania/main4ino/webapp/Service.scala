package org.mauritania.main4ino.webapp

import cats.effect.IO
import org.http4s.{HttpService, Request, Response, StaticFile}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.staticcontent
import org.http4s.server.staticcontent.ResourceService.Config

class Service(resourceIndexHtml: String) extends Http4sDsl[IO] {

  private val StaticResource = staticcontent.resourceService[IO](Config(basePath = "/webapp", pathPrefix = "/", cacheStrategy = staticcontent.MemoryCache()))

  val service = HttpService[IO] {

      case a@GET -> Root =>
        StaticFile.fromResource(resourceIndexHtml, Some(a)).getOrElseF(InternalServerError())

      case a@GET -> _ =>
        StaticResource(a).value.map(_.getOrElse(Response.notFound[IO]))

    }

	private[webapp] def request(r: Request[IO]): IO[Response[IO]] = service.orNotFound(r)

}

