package org.mauritania.main4ino

import java.io.File
import java.nio.file.Paths
import java.util.concurrent._

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Timer}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.mauritania.main4ino.db.Repository.ReqType
import org.mauritania.main4ino.api.{Translator, v1}
import org.mauritania.main4ino.db.{Cleaner, Database, Repository}
import org.mauritania.main4ino.firmware.Store
import org.mauritania.main4ino.helpers.{ConfigLoader, Scheduler, Time}
import org.mauritania.main4ino.security.{Auther, MethodRight}

import scala.concurrent.duration.FiniteDuration
import cats.effect._
import cats.implicits._
import org.http4s.server.{Router, Server => H4Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import doobie.util.ExecutionContexts
import org.mauritania.main4ino.devicelogs.Logger
import pureconfig._
import pureconfig.generic.auto._
import eu.timepit.refined.pureconfig._

object Server extends IOApp {

  import ConfigLoader.PureConfigImplicits._

  def createServer[F[_]: ContextShift: ConcurrentEffect: Timer: Sync: Async]: Resource[F, H4Server[F]] = for {
    logger <- Resource.liftF(Slf4jLogger.fromClass[F](getClass))
    _ <- Resource.liftF(logger.info(s"Initializing server..."))
    // Thread Pools - https://gist.github.com/djspiewak/46b543800958cf61af6efa8e072bfd5c
    blockingIoEc <- ExecutionContexts.cachedThreadPool[F]
    _ <- Resource.liftF(logger.debug(s"Thread pools initialized..."))

    args <- Resource.liftF[F, Args](ConfigLoader.fromEnv[F, Args])
    applicationConf = new File(args.configDir.toFile, "application.conf")
    securityConf = new File(args.configDir.toFile, "security.conf")
    configApp <- Resource.liftF(ConfigLoader.fromFile[F, Config](applicationConf))
    configUsers <- Resource.liftF(ConfigLoader.fromFile[F, security.Config](securityConf))
    _ <- Resource.liftF(logger.debug(s"Configurations created..."))

    transactor <- Database.transactor[F](configApp.database, blockingIoEc, Blocker.liftExecutionContext(blockingIoEc))
    _ <- Resource.liftF(logger.debug(s"Database initialized..."))

    auth = new Auther[F](configUsers)
    repo = new Repository[F](transactor)
    time = new Time[F]()
    cleaner = new Cleaner[F](repo, time)
    devLogger = new Logger(configApp.devLogger, time, blockingIoEc)
    _ <- Resource.liftF(logger.debug(s"Device logger initialized..."))

    fwStore = new Store(Paths.get(configApp.firmware.firmwareBasePath))
    _ <- Resource.liftF(logger.debug(s"Firmware store initialized..."))
    firmwareService = new firmware.Service(fwStore, blockingIoEc)

    httpApp = Router(
      "/" -> new webapp.Service("/webapp/index.html", blockingIoEc).service,
      "/" -> firmwareService.service,
      "/api/v1" -> new v1.Service(auth, new Translator(repo, time, devLogger), time, firmwareService).serviceWithAuthentication
    ).orNotFound
    _ <- Resource.liftF(logger.debug(s"Router initialized..."))

    _ <- Resource.liftF(Database.initialize(transactor))
    _ <- Resource.liftF(logger.debug(s"Database initialized..."))

    cleanupRepoTask = cleaner.cleanupRepo(configApp.database.cleanup.retentionSecs)
    cleanupPeriodSecs = FiniteDuration(configApp.database.cleanup.periodSecs.value, TimeUnit.SECONDS)
    _ <- Resource.liftF(Concurrent[F].start(Scheduler.periodic[F, Int](cleanupPeriodSecs, cleanupRepoTask)))
    _ <- Resource.liftF(logger.debug(s"Cleanup task initialized..."))

    exitCodeServer <- BlazeServerBuilder[F]
      .bindHttp(configApp.server.port.value, configApp.server.host)
      .withHttpApp(httpApp)
      .resource
    _ <- Resource.liftF(logger.info(s"Server initialized."))

  } yield exitCodeServer

  def run(args: List[String]): IO[ExitCode] = createServer[IO].use(_ => IO.never).as(ExitCode.Success)
}
