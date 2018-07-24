package org.mauritania.botinobe

import cats.effect.{Effect, IO}
import fs2.StreamApp
import org.http4s.server.blaze.BlazeBuilder
import org.mauritania.botinobe.api.v1.Service

import scala.concurrent.ExecutionContext

object Server extends StreamApp[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: IO[Unit]) = ServerStream.stream
}

object ServerStream {

  def serviceApiV1 = Service.service

  def stream(implicit ec: ExecutionContext) =
    BlazeBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .mountService(serviceApiV1, "/v1/")
      .serve
}
