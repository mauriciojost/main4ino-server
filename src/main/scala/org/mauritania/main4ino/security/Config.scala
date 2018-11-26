package org.mauritania.main4ino.security

import cats.effect.IO
import org.mauritania.main4ino.config.Loadable
import org.reactormonk.{CryptoBits, PrivateKey}
import java.time.Clock

import org.mauritania.main4ino.security.Authentication.{EncryptionConfig, UsersBy}

import scala.io.Codec


case class Config(
  users: List[User],
  privateKey: String,
  salt: String
) {

  val usersBy = UsersBy(users)
  val privateKeyBits = CryptoBits(PrivateKey(Codec.toUTF8(privateKey)))
  val nonceStartupTime = Clock.systemUTC()
  val encryptionConfig = EncryptionConfig(privateKeyBits, salt)
}

object Config extends Loadable[Config] {

  def load(configFile: String): IO[Config] = load(configFile, identity)

}

