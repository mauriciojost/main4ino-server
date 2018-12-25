package org.mauritania.main4ino.cli

import java.nio.file.Paths

import cats.Monad
import cats.effect.IO
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.mauritania.main4ino.cli.Algebras.{Configs, Console, Filesystem, Users}
import org.mauritania.main4ino.cli.Modules.{ConfigsAppErr, ConsoleSync, FilesystemSync, UsersSync}

object Client {

  def start[F[_] : Monad](C: Console[F], O: Configs[F], U: Users[F], S: Filesystem[F]): F[Unit] = {
    for {
      confc <- S.readFile(Paths.get("src", "main", "resources", "security.conf"))
      conf <- O.fromString(confc)
      _ <- C.writeLine("Enter user: ")
      rawUser <- C.readLine()
      user <- U.readRawUser(rawUser)
      outputFile = "security.output.conf"
      newConf <- O.addUser(conf, user)
      _ <- C.writeLine(s"Writing to $outputFile")
      newConfStr <- O.asString(newConf)
      _ <- S.writeFile(Paths.get(outputFile), newConfStr)
      _ <- C.writeLine("Done!")
    } yield ()
  }

  def main(args: Array[String]): Unit = {
    val cs = new ConsoleSync[IO]
    val co = new ConfigsAppErr[IO]
    val us = new UsersSync[IO]
    val fs = new FilesystemSync[IO]
    start[IO](cs, co, us, fs).unsafeRunSync()
  }
}
