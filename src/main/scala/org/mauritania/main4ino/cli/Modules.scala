package org.mauritania.main4ino.cli

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.Path

import cats.effect.Sync
import cats.{ApplicativeError, Monad}
import com.typesafe.config.{ConfigFactory, Config => TypeSafeConfig}
import io.circe.generic.auto._
import io.circe.syntax._
import org.mauritania.main4ino.cli.Algebras._
import org.mauritania.main4ino.cli.Actions.{AddRawUser, CliAction}
import org.mauritania.main4ino.security.{Authentication, Config, User}
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.mauritania.main4ino.config.Loadable
import pureconfig.error.ConfigReaderException

import scala.io.Source
import scala.util.Try
import pureconfig._
import pureconfig.generic.auto._

object Modules {

  class ConfigsAppErr[F[_]: Monad](implicit A: ApplicativeError[F, Throwable]) extends Configs[F] {

    def performAction(c: Config, action: CliAction): F[Config] = {
      A.pure {
        action match {
          case ru : Actions.AddRawUser => {
            val nUser: User = user(c, ru)
            val nConf = c.copy(users = nUser :: c.users)
            nConf
          }
          case _ => c
        }
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
