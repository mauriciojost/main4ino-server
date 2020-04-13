package org.mauritania.main4ino.security.confgen

import cats.effect.{IO, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.mauritania.main4ino.security.confgen.Algebras.{Configs, Filesystem}
import org.mauritania.main4ino.security.confgen.Modules.{ConfigsMonad, FilesystemSync}
import org.mauritania.main4ino.helpers.ConfigLoader
import org.mauritania.main4ino.security.Config
import pureconfig._
import pureconfig.generic.auto._

object Client {

  import ConfigLoader.PureConfigImplicits._

  def start[F[_]: Sync](O: Configs[F], S: Filesystem[F]): F[Unit] = {
    for {
      logger <- Slf4jLogger.fromClass[F](getClass)
      args <- ConfigLoader.fromEnv[F, Args]
      _ <- logger.debug(s"Args: $args")
      conf <- ConfigLoader.fromFile[F, Config](args.input.toFile)
      _ <- logger.debug(s"Configuration: $conf")
      actions <- ConfigLoader.fromFile[F, Actions](args.modif.toFile)
      _ <- logger.debug(s"Actions: $actions")
      newConf <- O.performActions(conf, actions.merged)
      newConfStr = O.asString(newConf)
      _ <- S.writeFile(args.output, newConfStr)
    } yield ()
  }

  def main(args: Array[String]): Unit = {
    val co = new ConfigsMonad[IO]
    val fs = new FilesystemSync[IO]
    start[IO](co, fs).unsafeRunSync()
  }
}
