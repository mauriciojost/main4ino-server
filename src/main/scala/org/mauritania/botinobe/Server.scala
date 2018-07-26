package org.mauritania.botinobe

import cats.effect.IO
import config.Config
import db.Database
import fs2.{Stream, StreamApp}
import fs2.StreamApp.ExitCode
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder
import org.mauritania.botinobe.api.v1
import cats.effect.IO
import config.Config
import db.Database
import fs2.{Stream, StreamApp}
import fs2.StreamApp.ExitCode
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder
import scala.concurrent.ExecutionContext.Implicits.global

object Server extends StreamApp[IO] with Http4sDsl[IO] {
  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] = {
    for {
      config <- Stream.eval(Config.load())
      transactor <- Stream.eval(Database.transactor(config.database))
      _ <- Stream.eval(Database.initialize(transactor))
      exitCode <- BlazeBuilder[IO]
        .bindHttp(config.server.port, config.server.host)
        .mountService(new v1.Service(new Repository(transactor)).service, "/v1/")
        .serve
    } yield exitCode
  }
}
