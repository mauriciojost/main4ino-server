package org.mauritania.main4ino.security

import java.io.File

import cats.effect.IO
import org.mauritania.main4ino.config.Loadable
import java.time.Clock

import tsec.common._
import pureconfig._
import pureconfig.generic.auto._
import org.mauritania.main4ino.security.Auther.{EncryptionConfig, UserHashedPass, UserId}
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import scala.io.Codec


case class Config(
  users: List[User],
  privatekey: String,
  salt: String
) {

  import Config._

  val usersBy = UsersBy(users)
  val privateKeyBits = privatekey.utf8Bytes
  val nonceStartupTime = Clock.systemUTC()
  val encryptionConfig = EncryptionConfig(privateKeyBits, salt)
}

object Config {

  def load(configFile: File): IO[Config] =
    Loadable.loadFromFile[IO, Config](configFile)

  case class UsersBy(
    byId: Map[UserId, User],
    byIdPass: Map[(UserId, UserHashedPass), User]
  )

  object UsersBy {
    def apply(u: List[User]): UsersBy = {
      UsersBy(
        byId = u.groupBy(_.name).map { case (t, us) => (t, us.last) },
        byIdPass = u.groupBy(i => (i.name, PasswordHash[BCrypt](i.hashedpass))).map { case (t, us) => (t, us.last) }
      )
    }
  }

}

