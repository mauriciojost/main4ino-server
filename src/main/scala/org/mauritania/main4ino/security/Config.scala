package org.mauritania.main4ino.security

import java.io.File

import cats.effect.Sync
import java.time.Clock
import org.mauritania.main4ino.helpers.ConfigLoader
import cats.effect.IO
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
  privatekey: String // TODO use Byte[Array]
) {

  import Config._

  val usersBy = UsersBy(users)
  val privateKeyBits = privatekey.utf8Bytes
  val encryptionConfig = EncryptionConfig(privateKeyBits)
}

object Config {

  case class UsersBy(
    byId: Map[UserId, User]
  )

  object UsersBy {
    def apply(u: List[User]): UsersBy = {
      UsersBy(
        byId = u.groupBy(_.name).map { case (t, us) => (t, us.last) }
      )
    }
  }

}

