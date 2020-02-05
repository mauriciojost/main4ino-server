package org.mauritania.main4ino.security.confgen

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.Path

import cats.effect.Sync
import cats._
import cats.syntax._
import cats.implicits._
import cats.instances._
import com.typesafe.config.{ConfigFactory, Config => TypeSafeConfig}
import io.circe.generic.auto._
import io.circe.syntax._
import org.mauritania.main4ino.security.confgen.Algebras._
import org.mauritania.main4ino.security.confgen.Actions.{AddRawUser, Action}
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
import tsec.passwordhashers.PasswordHasher
import tsec.passwordhashers.jca.BCrypt

object Modules {

  import ConfigLoader.CirceImplicits._
  import ConfigLoader.PureConfigImplicits._

  class ConfigsMonad[F[_]: Monad](implicit H: PasswordHasher[F, BCrypt]) extends Configs[F] {

    def performAction(c: Config, action: Action): F[Config] = {
      action match {
        case rus : Actions.AddRawUsers => {
          val nUsers: F[List[User]] = rus.users.map(u => user(c, u)).sequence
          val nConf = Monad[F].map(nUsers)(nu => c.copy(users = nu ++ c.users))
          nConf
        }
        case _ => Monad[F].pure(c)
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
