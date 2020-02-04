package org.mauritania.main4ino.security.confgen

import java.nio.file.{Path, Paths}

import cats.Monad
import cats.effect.{IO, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import enumeratum.Circe
import org.mauritania.main4ino.security.confgen.Algebras.{Configs, Filesystem}
import org.mauritania.main4ino.security.confgen.Actions.{AddRawUser, AddRawUsers}
import org.mauritania.main4ino.security.confgen.Modules.{ConfigsAppErr, FilesystemSync}
import org.mauritania.main4ino.helpers.ConfigLoader
import org.mauritania.main4ino.security.{Config, MethodRight}
import pureconfig._
import pureconfig.generic.auto._

object Client {

  import ConfigLoader.PureConfigImplicits._

  def start[F[_] : Sync](O: Configs[F], S: Filesystem[F]): F[Unit] = {
    for {
      args <- ConfigLoader.fromEnv[F, Args]
      conf <- ConfigLoader.fromFile[F, Config](args.input.toFile)
       // TODO support other actions too
      user <- ConfigLoader.fromFile[F, AddRawUsers](args.modif.toFile)
      newConf <- O.performAction(conf, user)
      newConfStr = O.asString(newConf)
      _ <- S.writeFile(args.output, newConfStr)
    } yield ()
  }

  def main(args: Array[String]): Unit = {
    val co = new ConfigsAppErr[IO]
    val fs = new FilesystemSync[IO]
    start[IO](co, fs).unsafeRunSync()
  }
}
