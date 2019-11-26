package org.mauritania.main4ino.security

import java.io.File

import cats.effect.Sync
import org.mauritania.main4ino.config.Loadable
import org.reactormonk.{CryptoBits, PrivateKey}
import java.time.Clock

import pureconfig._
import pureconfig.generic.auto._
import org.mauritania.main4ino.security.Auther.{EncryptionConfig, UserHashedPass, UserId}

import scala.io.Codec


case class Config(
  users: List[User],
  privatekey: String,
  salt: String
) {

  import Config._

  val usersBy = UsersBy(users)
  val privateKeyBits = CryptoBits(PrivateKey(Codec.toUTF8(privatekey)))
  val nonceStartupTime = Clock.systemUTC()
  val encryptionConfig = EncryptionConfig(privateKeyBits, salt)
}

object Config {

  def load[F[_]: Sync](configFile: File): F[Config] =
    Loadable.loadFromFile[F, Config](configFile)

  case class UsersBy(
    byId: Map[UserId, User],
    byIdPass: Map[(UserId, UserHashedPass), User]
  )

  object UsersBy {
    def apply(u: List[User]): UsersBy = {
      UsersBy(
        byId = u.groupBy(_.name).map { case (t, us) => (t, us.last) },
        byIdPass = u.groupBy(i => (i.name, i.hashedpass)).map { case (t, us) => (t, us.last) }
      )
    }
  }

}

