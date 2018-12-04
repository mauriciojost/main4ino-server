package org.mauritania.main4ino.cli

import java.nio.file.Path

import org.mauritania.main4ino.cli.Data.RawUser
import org.mauritania.main4ino.security.Config

object Algebras {

  trait Cli[F[_]]  {
    def readLine(msg: String): F[String]
  }

  trait Users[F[_]] {
    def readRawUser(s: String): F[RawUser]
  }

  trait Configs[F[_]]  {
    def addUser(c: Config, u: RawUser): F[Config]
    def readConfig(p: Path): F[Config]
    def writeConfig(c: Config, p: Path): F[Unit]
  }

}
