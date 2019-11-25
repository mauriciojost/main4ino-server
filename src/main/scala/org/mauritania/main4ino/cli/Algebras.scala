package org.mauritania.main4ino.cli

import java.nio.file.Path

import cats.Monad
import cats.implicits._
import org.mauritania.main4ino.cli.Actions.{AddRawUser, CliAction}
import org.mauritania.main4ino.security.{Auther, Config, User}
import tsec.passwordhashers._
import tsec.passwordhashers.jca._

object Algebras {

  trait Filesystem[F[_]]  {
    def readFile(p: Path): F[String]
    def writeFile(p: Path, b: String): F[Unit]
  }

  trait Configs[F[_]]  {

    def user(c: Config, u: AddRawUser)(implicit P: PasswordHasher[F, BCrypt], M: Monad[F]): F[User] = for {
      hashed <- Auther.hashPassword[F](u.pass, c.salt)
    } yield User(u.name, hashed, u.email, u.granted)

    def performAction(c: Config, a: CliAction): F[Config]
    def asString(c: Config): String
  }

}
