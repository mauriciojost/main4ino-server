package org.mauritania.main4ino.cli

import java.nio.file.{Path, Paths}

import cats.Monad
import cats.effect.{IO, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.mauritania.main4ino.cli.Algebras.{Configs, Filesystem}
import org.mauritania.main4ino.cli.Data.AddRawUserParams
import org.mauritania.main4ino.cli.Modules.{ConfigsAppErr, FilesystemSync}
import org.mauritania.main4ino.config.Loadable
import org.mauritania.main4ino.security.Config
import pureconfig._
import pureconfig.generic.auto._

object Client {

  def start[F[_] : Sync](O: Configs[F], S: Filesystem[F])(args: Array[String]): F[Unit] = {
    // TODO support parameters correctly
    val input = Paths.get(args(0))
    val modif = Paths.get(args(1))
    val output = Paths.get(args(2))
    for {
      conf <- Loadable.loadFromFile[F, Config](input.toFile)
       // TODO support other actions too
      user <- Loadable.loadFromFile[F, AddRawUserParams](modif.toFile)
      newConf <- O.addUser(conf, user)
      newConfStr <- O.asString(newConf)
      _ <- S.writeFile(output, newConfStr)
    } yield ()
  }

  def main(args: Array[String]): Unit = {
    val co = new ConfigsAppErr[IO]
    val fs = new FilesystemSync[IO]
    start[IO](co, fs)(args).unsafeRunSync()
  }
}
