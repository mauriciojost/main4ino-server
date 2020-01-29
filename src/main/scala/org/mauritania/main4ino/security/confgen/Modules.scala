package org.mauritania.main4ino.security.confgen

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.Path

import cats.effect.Sync
import cats.{ApplicativeError, Monad}
import com.typesafe.config.{ConfigFactory, Config => TypeSafeConfig}
import io.circe.generic.auto._
import io.circe.syntax._
import org.mauritania.main4ino.security.confgen.Algebras._
import org.mauritania.main4ino.security.confgen.Actions.{AddRawUser, CliAction}
import org.mauritania.main4ino.security.{Auther, Config, MethodRight, User}
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.mauritania.main4ino.helpers.ConfigLoader
import pureconfig.error.ConfigReaderException

import scala.io.Source
import scala.util.Try
import pureconfig._
import pureconfig.generic.auto._
import enumeratum._
import io.circe.Encoder

object Modules {

  import ConfigLoader.CirceImplicits._
  import ConfigLoader.PureConfigImplicits._

  class ConfigsAppErr[F[_]: Monad](implicit A: ApplicativeError[F, Throwable]) extends Configs[F] {

    def performAction(c: Config, action: CliAction): Config = {
      action match {
        case rus : Actions.AddRawUsers => {
          val nUsers: List[User] = rus.users.map(u => user(c, u))
          val nConf = c.copy(users = nUsers ++ c.users)
          nConf
        }
        case _ => c
      }
    }

    def asString(c: Config): String = c.asJson.noSpaces
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
