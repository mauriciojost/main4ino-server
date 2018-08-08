package org.mauritania.botinobe.webapp

import cats.effect.IO
import org.http4s.{Request, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpService
import org.http4s.server.staticcontent
import org.http4s.server.staticcontent.ResourceService.Config

class Service() extends Http4sDsl[IO] {


  private val static = staticcontent.resourceService[IO](Config(basePath = "/webapp", pathPrefix = "/", cacheStrategy = staticcontent.MemoryCache()))

  val service = HttpService[IO] {

      case a@GET -> _ => {
        static(a).value.map(_.getOrElse(Response.notFound[IO]))
      }

    }

	private[webapp] def request(r: Request[IO]): IO[Response[IO]] = service.orNotFound(r)

}

object Service {

  final val ServicePrefix = "/webapp"

}
