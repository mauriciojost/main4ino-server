package org.mauritania.main4ino.cli

import java.nio.file.Path

import org.mauritania.main4ino.cli.Data.RawUser
import org.mauritania.main4ino.security.Config

object Algebras {

  trait Console[F[_]]  {
    def readLine(): F[String]
    def writeLine(msg: String): F[Unit]
  }

  trait Filesystem[F[_]]  {
    def readFile(p: Path): F[String]
    def writeFile(p: Path, b: String): F[Unit]
  }

  trait Users[F[_]] {
    def readRawUser(s: String): F[RawUser]
  }

  trait Configs[F[_]]  {
    def addUser(c: Config, u: RawUser): F[Config]
    def fromString(b: String): F[Config]
    def asString(c: Config): F[String]
  }

}
