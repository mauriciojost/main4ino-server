package org.mauritania.main4ino.cli

import java.nio.file.Paths

import cats.Monad
import cats.effect.IO
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.mauritania.main4ino.cli.Algebras.{Configs, Console, Users}
import org.mauritania.main4ino.cli.Modules.{ConfigsSync, ConsoleSync, UsersSync}

object Client {

  def start[F[_] : Monad](C: Console[F], O: Configs[F], U: Users[F]): F[Unit] = {
    for {
      conf <- O.readConfig(Paths.get("src", "main", "resources", "security.conf"))
      _ <- C.writeLine("Enter user: ")
      rawUser <- C.readLine()
      user <- U.readRawUser(rawUser)
      outputFile = "security.output.conf"
      newConf <- O.addUser(conf, user)
      _ <- C.writeLine(s"Writing to $outputFile")
      _ <- O.writeConfig(newConf, Paths.get(outputFile))
      _ <- C.writeLine("Done!")
    } yield ()
  }

  def main(args: Array[String]): Unit = {
    val cs = new ConsoleSync[IO]
    val co = new ConfigsSync[IO]
    val us = new UsersSync[IO]
    start[IO](cs, co, us).unsafeRunSync()
  }
}
