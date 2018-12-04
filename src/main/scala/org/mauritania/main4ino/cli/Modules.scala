package org.mauritania.main4ino.cli

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.Path

import cats.effect.Sync
import com.typesafe.config.ConfigFactory

import scala.io.StdIn
import org.mauritania.main4ino.cli.Algebras._
import org.mauritania.main4ino.cli.Data.RawUser
import org.mauritania.main4ino.config.Loadable
import org.mauritania.main4ino.security.{Authentication, Config, User}
import pureconfig.error.ConfigReaderException
import io.circe.syntax._
import io.circe.generic.auto._

object Modules {

  class CliSync[F[_]: Sync] extends Cli[F]  {
    def readLine(msg: String): F[String] = {
      Sync[F].delay {
        println(msg)
        StdIn.readLine()
      }
    }
  }

  class UsersSync[F[_]: Sync] extends Users[F] {
    def readRawUser(s: String): F[RawUser] = {
      Sync[F].delay {
        s.split(" ").toList match {
          case user :: pass :: email :: granted => RawUser(user, pass, email, granted)
          case _ => throw new IllegalArgumentException("Invalid amount of arguments: user pass email grant1 grant2 ...")
        }

      }
    }
  }

  class ConfigsSync[F[_]: Sync] extends Configs[F] with Loadable {
    import pureconfig._
    def addUser(c: Config, u: RawUser): F[Config] = {
      Sync[F].delay {
        val hashed = Authentication.hashPassword(u.pass, c.salt)
        val nUser = User(u.name, hashed, u.email, u.granted)
        val nConf = c.copy(users = nUser :: c.users)
        nConf
      }
    }

    def readConfig(p: Path): F[Config] = {
      Sync[F].delay {
        implicit val ph = ProductHint[Config](ConfigFieldMapping(CamelCase, CamelCase))
        loadConfig[Config](ConfigFactory.load(p.toAbsolutePath.toString)) match {
          case Left(e) => throw new ConfigReaderException[Config](e)
          case Right(config) => config
        }
      }
    }

    def writeConfig(c: Config, p: Path): F[Unit] = {
      Sync[F].delay {
        val bw = new BufferedWriter(new FileWriter(p.toFile))
        try bw.write(c.asJson.noSpaces) finally bw.close()
      }
    }
  }

}
