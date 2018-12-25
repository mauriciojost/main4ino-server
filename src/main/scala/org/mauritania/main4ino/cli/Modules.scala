package org.mauritania.main4ino.cli

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.Path

import cats.effect.Sync
import cats.{ApplicativeError, Monad}
import com.typesafe.config.{ConfigFactory, Config => TypeSafeConfig}
import io.circe.generic.auto._
import io.circe.syntax._
import org.mauritania.main4ino.cli.Algebras._
import org.mauritania.main4ino.cli.Data.RawUser
import org.mauritania.main4ino.security.{Authentication, Config, User}
import cats.syntax.flatMap._
import cats.syntax.functor._

import scala.io.{Source, StdIn}
import scala.util.Try

object Modules {

  class ConsoleSync[F[_] : Sync] extends Console[F] {
    def readLine(): F[String] = {
      Sync[F].delay {
        StdIn.readLine()
      }
    }
    def writeLine(msg: String): F[Unit] = {
      Sync[F].delay {
        println(msg)
      }
    }
  }

  class UsersSync[F[_] : Sync] extends Users[F] {
    def readRawUser(s: String): F[RawUser] = {
      Sync[F].delay {
        s.split(" ").toList match {
          case user :: pass :: email :: granted => RawUser(user, pass, email, granted)
          case _ => throw new IllegalArgumentException("Invalid amount of arguments: user pass email grant1 grant2 ...")
        }

      }
    }
  }

  class ConfigsAppErr[F[_]: Monad](implicit A: ApplicativeError[F, Throwable]) extends Configs[F] {

    def addUser(c: Config, u: RawUser): F[Config] = {
      A.pure {
        val hashed = Authentication.hashPassword(u.pass, c.salt)
        val nUser = User(u.name, hashed, u.email, u.granted)
        val nConf = c.copy(users = nUser :: c.users)
        nConf
      }
    }

    def fromString(s: String): F[Config] = {
      val c = ConfigFactory.parseString(s)
      val c1 = pureconfig.loadConfig[Config](c: TypeSafeConfig)
      c1 match {
        case Right(v) => A.pure(v)
        case Left(e) => A.raiseError(new IllegalArgumentException(e.toList.map(_.description).mkString(". ")))
      }
    }

    def asString(c: Config): F[String] = A.pure(c.asJson.noSpaces)
  }

  class FilesystemSync[F[_]](implicit S: Sync[F]) extends Filesystem[F]  {
    def readFile(p: Path): F[String] = S.fromTry[String](Try(Source.fromFile(p.toFile).mkString))

    def writeFile(p: Path, b: String): F[Unit] = {
      S.fromTry[Unit] {
        Try {
          val bw = new BufferedWriter(new FileWriter(p.toFile))
          try bw.write(b) finally bw.close()
        }
      }

    }
  }

}
