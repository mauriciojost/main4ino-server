package org.mauritania.main4ino.cli

import java.nio.file.Paths

import cats.effect.{IO, Sync}
import cats.syntax.functor._, cats.syntax.flatMap._
import org.mauritania.main4ino.cli.Algebras.{Cli, Configs, Users}
import org.mauritania.main4ino.cli.Modules.{CliSync, ConfigsSync, UsersSync}

object Client {

  def start[F[_]: Sync: Cli: Configs: Users](): F[Unit] = {
    for {
      c <- implicitly[Configs[F]].readConfig(Paths.get("src", "main", "resources", "security.conf"))
      l <- implicitly[Cli[F]].readLine("Enter user: ")
      u <- implicitly[Users[F]].readRawUser(l)
      n <- implicitly[Configs[F]].addUser(c, u)
      n <- implicitly[Configs[F]].writeConfig(n, Paths.get("security.output.conf"))
    } yield (())
  }

  def main(args: Array[String]): Unit = {
    implicit val cs = new CliSync[IO]()
    implicit val co = new ConfigsSync[IO]()
    implicit val us = new UsersSync[IO]()
    start[IO]().unsafeRunSync()
  }
}
