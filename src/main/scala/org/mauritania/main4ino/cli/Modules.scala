package org.mauritania.main4ino.cli

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.Path

import cats.ApplicativeError
import cats.effect.Sync
import com.typesafe.config.ConfigFactory
import io.circe.generic.auto._
import io.circe.syntax._
import org.mauritania.main4ino.cli.Algebras._
import org.mauritania.main4ino.cli.Data.RawUser
import org.mauritania.main4ino.config.Loadable
import org.mauritania.main4ino.security.{Authentication, Config, User}
import pureconfig.error.ConfigReaderException

import scala.io.StdIn
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

  class ConfigsSync[F[_]](implicit A: ApplicativeError[F, Throwable]) extends Configs[F] with Loadable {

    def addUser(c: Config, u: RawUser): F[Config] = {
      A.pure {
        val hashed = Authentication.hashPassword(u.pass, c.salt)
        val nUser = User(u.name, hashed, u.email, u.granted)
        val nConf = c.copy(users = nUser :: c.users)
        nConf
      }
    }

    def readConfig(p: Path): F[Config] = {
      import pureconfig._
      //implicit val ph: ProductHint[Config] = ProductHint[Config](ConfigFieldMapping(CamelCase, CamelCase))
      val absPath = p.toFile
      val c = ConfigFactory.parseFile(absPath)
      loadConfig[Config](c) match {
        case Left(e) => A.raiseError(new ConfigReaderException[Config](e))
        case Right(config) => A.pure(config)
      }
    }

    def writeConfig(c: Config, p: Path): F[Unit] = {
      A.fromTry[Unit] {
        Try {
          val bw = new BufferedWriter(new FileWriter(p.toFile))
          try bw.write(c.asJson.noSpaces) finally bw.close()
        }
      }
    }
  }

}
