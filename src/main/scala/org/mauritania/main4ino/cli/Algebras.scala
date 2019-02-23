package org.mauritania.main4ino.cli

import java.nio.file.Path

import org.mauritania.main4ino.cli.Data.AddRawUserParams
import org.mauritania.main4ino.security.{Authentication, Config, User}

object Algebras {

  trait Filesystem[F[_]]  {
    def readFile(p: Path): F[String]
    def writeFile(p: Path, b: String): F[Unit]
  }

  trait Configs[F[_]]  {

    def user(c: Config, u: AddRawUserParams): User = {
      val hashed = Authentication.hashPassword(u.pass, c.salt)
      User(u.name, hashed, u.email, u.granted)
    }

    def addUser(c: Config, u: AddRawUserParams): F[Config]
    def asString(c: Config): F[String]
  }

}
