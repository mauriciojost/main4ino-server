package org.mauritania.main4ino

import java.io.File

import cats.effect.IO
import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}
import org.http4s.server.blaze.BlazeBuilder
import org.mauritania.main4ino.api.v1
import org.mauritania.main4ino.db.Database
import org.mauritania.main4ino.security.AuthenticationIO

import scala.concurrent.ExecutionContext.Implicits.global

object Server extends StreamApp[IO] {

  // TODO use better IOApp as StreamApp is being removed from fs2
  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] = {
    val dir = new File(args(0)) // TODO use CLI arguments
    for {
      configApp <- Stream.eval(config.Config.load(new File(dir, "application.conf")))
      configUsers <- Stream.eval(security.Config.load(new File(dir, "security.conf")))
      transactor <- Stream.eval(Database.transactor(configApp.database))
      _ <- Stream.eval(Database.initialize(transactor))
      exitCode <- BlazeBuilder[IO]
        .bindHttp(configApp.server.port, configApp.server.host)
        .mountService(new webapp.Service().service, "/")
        .mountService(new v1.Service(new AuthenticationIO(configUsers), new RepositoryIO(transactor)).serviceWithAuthentication, "/api/v1")
        .serve
    } yield exitCode
  }
}
