package org.mauritania.main4ino.webapp

import cats.implicits._
import cats.effect.{Blocker, ContextShift, Effect, Sync}
import org.http4s.{HttpRoutes, Request, Response, StaticFile}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.staticcontent
import org.http4s.server.staticcontent.ResourceService.Config
import org.mauritania.main4ino.helpers.HttpMeter

import scala.concurrent.ExecutionContext

class Service[F[_]: Effect: Sync: ContextShift](resourceIndexHtml: String, ec: ExecutionContext) extends Http4sDsl[F] {

  final private lazy val blocker = Blocker.liftExecutionContext(ec)

  final private lazy val StaticResource = staticcontent.resourceService[F](
    Config(
      basePath = "/webapp",
      pathPrefix = "/",
      cacheStrategy = staticcontent.MemoryCache(),
      blocker = blocker
    )
  )

  val serviceUntimed = HttpRoutes.of[F] {

    case a @ GET -> Root =>
      StaticFile.fromResource(resourceIndexHtml, blocker, Some(a)).getOrElseF(InternalServerError())

    case a @ GET -> _ =>
      StaticResource(a).value.map(_.getOrElse(Response.notFound[F]))

  }

  val service = HttpMeter.timedHttpMiddleware[F].apply(serviceUntimed)

  private[webapp] def request(r: Request[F]): F[Response[F]] = service(r).getOrElseF(NotFound())

}
