package org.mauritania.main4ino

import java.io.File
import java.nio.file.Paths
import java.util.concurrent._

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Timer}
import org.mauritania.main4ino.db.Repository.ReqType
import org.mauritania.main4ino.api.{v1, Translator}
import org.mauritania.main4ino.db.{Cleaner, Database, Repository}
import org.mauritania.main4ino.firmware.{Service, Store}
import org.mauritania.main4ino.helpers.{ConfigLoader, Scheduler, Time}
import org.mauritania.main4ino.security.{Auther, MethodRight}

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

import scala.concurrent.ExecutionContext

object Server extends IOApp {

  import ConfigLoader.PureConfigImplicits._

  def create[F[_]: ContextShift: ConcurrentEffect: Timer: Sync: Async]: Resource[F, H4Server[F]] =
    for {
      args <- Resource.liftF[F, Args](ConfigLoader.fromEnv[F, Args])

      blockingIoEc <- ExecutionContexts.cachedThreadPool[F] // Thread Pools - https://gist.github.com/djspiewak/46b543800958cf61af6efa8e072bfd5c

      configApp <- Resource.liftF(ConfigLoader.fromFile[F, Config](new File(args.configDir.toFile, "application.conf")))
      configUsers <- Resource.liftF(ConfigLoader.fromFile[F, security.Config](new File(args.configDir.toFile, "security.conf")))

      transactor <- Database.transactor[F](configApp.database, blockingIoEc, Blocker.liftExecutionContext(blockingIoEc))

      repo = new Repository[F](transactor)
      time = new Time[F]()

      httpApp = router(configApp, configUsers, blockingIoEc, repo, time)

      _ <- Resource.liftF(Database.initialize(transactor))

      cleanupRepoTask = new Cleaner[F](repo, time).cleanupRepo(configApp.database.cleanup.retentionSecs)
      _ <- Resource.liftF(Concurrent[F].start(Scheduler.periodic[F, Int](configApp.database.cleanup.periodSecsFiniteDuration, cleanupRepoTask)))

      exitCodeServer <- BlazeServerBuilder[F]
        .bindHttp(configApp.server.port.value, configApp.server.host)
        .withHttpApp(httpApp)
        .resource

    } yield exitCodeServer

  private def router[F[_]: ContextShift: ConcurrentEffect: Timer: Sync: Async](
    configApp: Config,
    configUsers: security.Config,
    blockingIoEc: ExecutionContext,
    repo: Repository[F],
    time: Time[F]
  ) = {
    val devLogger = new Logger(configApp.devLogger, time, blockingIoEc)
    val fwStore = new Store(Paths.get(configApp.firmware.firmwareBasePath))
    val firmwareService = new firmware.Service(fwStore, blockingIoEc)
    Router(
      "/" -> new webapp.Service("/webapp/index.html", blockingIoEc).service,
      "/" -> firmwareService.service,
      "/api/v1" -> new v1.Service(
        new Auther[F](configUsers),
        new Translator(repo, time, devLogger),
        time,
        firmwareService
      ).serviceWithAuthentication
    ).orNotFound
  }

  def run(args: List[String]): IO[ExitCode] = create[IO].use(_ => IO.never).as(ExitCode.Success)
}
