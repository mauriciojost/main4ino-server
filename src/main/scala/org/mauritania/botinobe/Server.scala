package org.mauritania.botinobe

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}
import org.http4s.{AuthedService, Cookie, Request, Response, headers}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.server.blaze.BlazeBuilder
import org.mauritania.botinobe.api.v1
import org.mauritania.botinobe.config.Config
import org.mauritania.botinobe.db.Database
import org.reactormonk.{CryptoBits, PrivateKey}
import java.time._

import org.http4s.util.string._
import org.http4s.headers.Authorization
import org.reactormonk.{CryptoBits, PrivateKey}
import java.time._

import cats.syntax.either._

import scala.concurrent.ExecutionContext.Implicits.global

object Server extends StreamApp[IO] with Http4sDsl[IO] {

  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] = {
    for {
      config <- Stream.eval(Config.load())
      transactor <- Stream.eval(Database.transactor(config.database))
      _ <- Stream.eval(Database.initialize(transactor))
      exitCode <- BlazeBuilder[IO]
        .bindHttp(config.server.port, config.server.host)
        .mountService(new v1.Service(new Repository(transactor)).serviceSec, "/api/v1/")
        .serve
    } yield exitCode
  }
}
