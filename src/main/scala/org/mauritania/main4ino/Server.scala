package org.mauritania.main4ino

import java.util.concurrent._

import cats.effect.IO
import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.server.blaze.BlazeBuilder
import org.mauritania.main4ino.RepositoryIO.Table
import org.mauritania.main4ino.api.v1
import org.mauritania.main4ino.db.Database
import org.mauritania.main4ino.helpers.TimeIO
import org.mauritania.main4ino.security.AuthenticationIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

object Server extends StreamApp[IO] {

  // TODO use better IOApp as StreamApp is being removed from fs2
  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] = {
    for {
      configApp <- Stream.eval(config.Config.load("application.conf"))
      configUsers <- Stream.eval(security.Config.load("security.conf"))
      transactor <- Stream.eval(Database.transactor(configApp.database))
      auth = new AuthenticationIO(configUsers)
      repo = new RepositoryIO(transactor)
      time = new TimeIO()
      cleanupRepoTask = for {
        logger <- Slf4jLogger.fromClass[IO](Server.getClass)
        now <- time.nowUtc
        epSecs = now.toEpochSecond
        cleaned <- repo.cleanup(Table.Reports, epSecs, configApp.database.cleanup.retentionSecs)
        _ <- logger.info(s"Repository cleanup at $now ($epSecs): $cleaned cleaned")
      } yield (cleaned)

      _ <- Stream.eval(Database.initialize(transactor))
      cleanupPeriodSecs = FiniteDuration(configApp.database.cleanup.periodSecs, TimeUnit.SECONDS)
      _ <- Stream.eval(Scheduler.periodicIO(cleanupRepoTask, cleanupPeriodSecs))
      exitCodeServer <- BlazeBuilder[IO]
        .bindHttp(configApp.server.port, configApp.server.host)
        .mountService(new webapp.Service().service, "/")
        .mountService(new v1.Service(auth, repo, time).serviceWithAuthentication, "/api/v1")
        .serve

    } yield exitCodeServer
  }
}
