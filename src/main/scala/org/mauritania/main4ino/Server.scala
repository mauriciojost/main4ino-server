package org.mauritania.main4ino

import java.io.File
import java.nio.file.Paths
import java.util.concurrent._

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Timer}
import fs2.Stream
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.server.blaze.BlazeBuilder
import org.mauritania.main4ino.Repository.ReqType
import org.mauritania.main4ino.api.{Translator, v1}
import org.mauritania.main4ino.db.Database
import org.mauritania.main4ino.firmware.{Store, StoreIO}
import org.mauritania.main4ino.helpers.{DevLoggerIO, TimeIO}
import org.mauritania.main4ino.security.AutherIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration


import config._
import cats.effect._
import cats.implicits._
import org.http4s.server.{Router, Server => H4Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import doobie.util.ExecutionContexts

import scala.concurrent.ExecutionContext


object Server extends IOApp {

  def createServer(args: List[String]): Resource[IO, H4Server[IO]] = {

    for {
      ec <- ExecutionContexts.cachedThreadPool[IO]
      blocker = Blocker.liftExecutionContext(ec)

      configDir <- Resource.liftF[IO, File](resolveConfigDir(args))
      applicationConf = new File(configDir, "application.conf")
      securityConf = new File(configDir, "security.conf")

      configApp <- Resource.liftF[IO, config.Config](config.Config.load(applicationConf))
      configUsers <- Resource.liftF[IO, security.Config](security.Config.load(securityConf))
      transactor <- Resource.liftF(Database.transactor(configApp.database, ec, blocker))
      auth = new AutherIO(configUsers)
      repo = new RepositoryIO(transactor)
      time = new TimeIO()
      devLogger = new DevLoggerIO(Paths.get(configApp.devLogger.logsBasePath), time, blocker, ec)
      fwStore = new StoreIO(Paths.get(configApp.firmware.firmwareBasePath))
      cleanupRepoTask = for {
        logger <- Slf4jLogger.fromClass[IO](Server.getClass)
        now <- time.nowUtc
        epSecs = now.toEpochSecond
        cleaned <- repo.cleanup(ReqType.Reports, epSecs, configApp.database.cleanup.retentionSecs)
        _ <- logger.info(s"Repository cleanup at $now ($epSecs): $cleaned requests cleaned")
      } yield (cleaned)

      httpApp = Router(
        "/" -> new webapp.Service("/webapp/index.html", ec, blocker).service,
        "/" -> new firmware.Service(fwStore, blocker).service,
        "/api/v1" -> new v1.Service(auth, new Translator(repo, time, devLogger, fwStore), time).serviceWithAuthentication
      ).orNotFound

      _ <- Resource.liftF(Database.initialize(transactor))
      cleanupPeriodSecs = FiniteDuration(configApp.database.cleanup.periodSecs, TimeUnit.SECONDS)
      _ <- Resource.liftF(IO.sleep(cleanupPeriodSecs) *> cleanupRepoTask)
      exitCodeServer <- BlazeServerBuilder[IO]
        .bindHttp(configApp.server.port, configApp.server.host)
        .withHttpApp(httpApp)
        .resource
    } yield exitCodeServer
  }

  private def resolveConfigDir(args: List[String]): IO[File] = IO {
    val arg1 = args match {
      case Nil => throw new IllegalArgumentException("Missing config directory")
      case a1 :: Nil => a1
      case _ => throw new IllegalArgumentException("Too many arguments")
    }
    new File(arg1)
  }

  def run(args: List[String]): IO[ExitCode] = createServer(args).use(_ => IO.never).as(ExitCode.Success)
}
