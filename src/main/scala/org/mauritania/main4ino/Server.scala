package org.mauritania.main4ino

import java.io.File
import java.nio.file.Paths
import java.util.concurrent._

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Timer}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.mauritania.main4ino.Repository.ReqType
import org.mauritania.main4ino.api.{Translator, v1}
import org.mauritania.main4ino.db.Database
import org.mauritania.main4ino.firmware.{Store, StoreIO}
import org.mauritania.main4ino.helpers.{DevLoggerIO, TimeIO}
import org.mauritania.main4ino.security.AutherIO

import scala.concurrent.duration.FiniteDuration


import config._
import cats.effect._
import cats.implicits._
import org.http4s.server.{Router, Server => H4Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import doobie.util.ExecutionContexts

object Server extends IOApp {

  def createServer[F[_]: ContextShift: ConcurrentEffect: Timer: Sync: Async](args: List[String]): Resource[F, H4Server[F]] = {

    for {

      logger <- Resource.liftF(Slf4jLogger.fromClass[F](Translator.getClass))
      transactorEc <- ExecutionContexts.cachedThreadPool[F]
      blockerTransactorEc <- ExecutionContexts.cachedThreadPool[F]
      devLoggerEc <- ExecutionContexts.cachedThreadPool[F]
      webappServiceEc <- ExecutionContexts.cachedThreadPool[F]
      firmwareServiceEc <- ExecutionContexts.cachedThreadPool[F]
      _ <- Resource.liftF(logger.debug(s"Cached thread pools created"))
      configDir <- Resource.liftF[F, File](resolveConfigDir(args))
      applicationConf = new File(configDir, "application.conf")
      securityConf = new File(configDir, "security.conf")
      configApp <- Resource.liftF(config.Config.load(applicationConf))
      configUsers <- Resource.liftF(security.Config.load(securityConf))
      _ <- Resource.liftF(logger.debug(s"Configs created"))
      transactor <- Database.transactor[F](configApp.database, transactorEc, Blocker.liftExecutionContext(blockerTransactorEc))
      _ <- Resource.liftF(logger.debug(s"Transactor created"))
      auth = new AutherIO[F](configUsers)
      repo = new RepositoryIO[F](transactor)
      time = new TimeIO[F]()
      devLogger = new DevLoggerIO(Paths.get(configApp.devLogger.logsBasePath), time, devLoggerEc)
      _ <- Resource.liftF(logger.debug(s"DevLogger created"))
      fwStore = new StoreIO(Paths.get(configApp.firmware.firmwareBasePath))
      _ <- Resource.liftF(logger.debug(s"Store created"))
      /*
      cleanupRepoTask = for {
        logger <- Slf4jLogger.fromClass[F](Server.getClass)
        now <- time.nowUtc
        epSecs = now.toEpochSecond
        cleaned <- repo.cleanup(ReqType.Reports, epSecs, configApp.database.cleanup.retentionSecs)
        _ <- logger.info(s"Repository cleanup at $now ($epSecs): $cleaned requests cleaned")
      } yield (cleaned)
       */

      _ <- Resource.liftF(logger.debug(s"Cleanup created"))
      httpApp = Router(
        "/" -> new webapp.Service("/webapp/index.html", webappServiceEc).service,
        "/" -> new firmware.Service(fwStore, firmwareServiceEc).service,
        "/api/v1" -> new v1.Service(auth, new Translator(repo, time, devLogger, fwStore), time).serviceWithAuthentication
      ).orNotFound

      _ <- Resource.liftF(Database.initialize(transactor))
      _ <- Resource.liftF(logger.debug(s"Database initialized"))
      cleanupPeriodSecs = FiniteDuration(configApp.database.cleanup.periodSecs, TimeUnit.SECONDS)
      //_ <- Resource.liftF(Timer[F].sleep(cleanupPeriodSecs) *> cleanupRepoTask) // TODO BROKEN TEST
      _ <- Resource.liftF(logger.debug(s"Server initialized"))
      exitCodeServer <- BlazeServerBuilder[F]
        .bindHttp(configApp.server.port, configApp.server.host)
        .withHttpApp(httpApp)
        .resource
      _ <- Resource.liftF(logger.debug(s"Server initialized"))

    } yield exitCodeServer
  }

  private def resolveConfigDir[F[_]: Sync](args: List[String]): F[File] = Sync[F].delay {
    val arg1 = args match {
      case Nil => throw new IllegalArgumentException("Missing config directory")
      case a1 :: Nil => a1
      case _ => throw new IllegalArgumentException("Too many arguments")
    }
    new File(arg1)
  }

  def run(args: List[String]): IO[ExitCode] = createServer[IO](args).use(_ => IO.never).as(ExitCode.Success)
}
