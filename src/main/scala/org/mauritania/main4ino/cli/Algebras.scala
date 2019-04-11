package org.mauritania.main4ino.cli

import java.nio.file.Path

import org.mauritania.main4ino.cli.Actions.{AddRawUser, CliAction}
import org.mauritania.main4ino.security.{Auther, Config, User}

object Algebras {

  trait Filesystem[F[_]]  {
    def readFile(p: Path): F[String]
    def writeFile(p: Path, b: String): F[Unit]
  }

  trait Configs[F[_]]  {

    def user(c: Config, u: AddRawUser): User = {
      val hashed = Auther.hashPassword(u.pass, c.salt)
      User(u.name, hashed, u.email, u.granted)
    }

    def performAction(c: Config, a: CliAction): F[Config]
    def asString(c: Config): F[String]
  }

}
