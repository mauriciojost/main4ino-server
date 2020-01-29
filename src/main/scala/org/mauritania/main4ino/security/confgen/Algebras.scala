package org.mauritania.main4ino.security.confgen

import java.nio.file.Path

import org.mauritania.main4ino.security.confgen.Actions.{AddRawUser, CliAction}
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

   // No F[_] returned as they are not meant to have side effects
    def performAction(c: Config, a: CliAction): Config
    def asString(c: Config): String
  }

}
