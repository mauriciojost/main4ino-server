package org.mauritania.main4ino

import java.io.File
import java.util.concurrent._

import cats.effect.IO
import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.server.blaze.BlazeBuilder
import org.mauritania.main4ino.Repository.ReqType
import org.mauritania.main4ino.api.{Translator, v1}
import org.mauritania.main4ino.db.Database
import org.mauritania.main4ino.helpers.TimeIO
import org.mauritania.main4ino.security.AutherIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

// If I replaced the below F=IO by some other Sync that does NOT delay the execution of side effects,
// my program would stop having the property of referential transparency, correct? (side-effect expressions are evaluated
// immediately without waiting for the end of the world).
// What other Monad other than IO would it make sense to use here?
object Server extends StreamApp[IO] {
  final val DefaultConfigDir = "."

  // TODO use better IOApp as StreamApp is being removed from fs2
  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] = {
    val configDir = new File(args.headOption.getOrElse(DefaultConfigDir))

    val applicationConf = new File(configDir, "application.conf")
    val securityConf = new File(configDir, "security.conf")

    for {
      configApp <- Stream.eval(config.Config.load(applicationConf))
      configUsers <- Stream.eval(security.Config.load(securityConf))
      transactor <- Stream.eval(Database.transactor(configApp.database))
      auth = new AutherIO(configUsers)
      repo = new RepositoryIO(transactor)
      time = new TimeIO()
      // Periodic execution of tasks (like cleanup of old records from DB as in here)
      cleanupRepoTask = for {
        logger <- Slf4jLogger.fromClass[IO](Server.getClass)
        now <- time.nowUtc
        epSecs = now.toEpochSecond
        cleaned <- repo.cleanup(ReqType.Reports, epSecs, configApp.database.cleanup.retentionSecs)
        _ <- logger.info(s"Repository cleanup at $now ($epSecs): $cleaned requests cleaned")
      } yield (cleaned)

      _ <- Stream.eval(Database.initialize(transactor))
      cleanupPeriodSecs = FiniteDuration(configApp.database.cleanup.periodSecs, TimeUnit.SECONDS)
      _ <- Stream.eval(Scheduler.periodicIO(cleanupRepoTask, cleanupPeriodSecs))
      exitCodeServer <- BlazeBuilder[IO]
        .bindHttp(configApp.server.port, configApp.server.host)
        .mountService(new webapp.Service().service, "/")
        .mountService(new v1.Service(auth, new Translator(repo, time), time).serviceWithAuthentication, "/api/v1")
        .serve

    } yield exitCodeServer
  }
}
