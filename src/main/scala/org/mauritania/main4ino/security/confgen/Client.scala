package org.mauritania.main4ino.security.confgen

import java.nio.file.{Path, Paths}

import cats.Monad
import cats.effect.{IO, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import enumeratum.Circe
import org.mauritania.main4ino.security.confgen.Algebras.{Configs, Filesystem}
import org.mauritania.main4ino.security.confgen.Actions.{Action, AddRawUser, AddRawUsers}
import org.mauritania.main4ino.security.confgen.Modules.{ConfigsMonad, FilesystemSync}
import org.mauritania.main4ino.helpers.ConfigLoader
import org.mauritania.main4ino.security.{Config, MethodRight}
import pureconfig._
import pureconfig.generic.auto._

object Client {

  import ConfigLoader.PureConfigImplicits._

  def start[F[_]: Sync](O: Configs[F], S: Filesystem[F]): F[Unit] = {
    for {
      args <- ConfigLoader.fromEnv[F, Args]
      conf <- ConfigLoader.fromFile[F, Config](args.input.toFile)
      actions <- ConfigLoader.fromFile[F, Actions](args.modif.toFile)
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
