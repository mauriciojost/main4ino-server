package org.mauritania.main4ino.security

import cats.effect.IO
import org.mauritania.main4ino.config.Loadable
import org.reactormonk.{CryptoBits, PrivateKey}
import java.time.Clock


case class Config(
  users: List[User],
  privateKey: String
) {
  private def validatedUsers: Iterable[User] = {
    val byToken = users.groupBy(_.token)
    val withSingleUser = byToken.collect {
      case (_, users) if users.length == 1 => users.head
      case (_, users) if users.length != 1 => throw new IllegalArgumentException(s"Multiple with similar token: $users")
    }
    withSingleUser
  }

  val usersByToken = validatedUsers.groupBy(_.token).map { case (t, us) => (t, us.head) }
  val privateKeyBits = CryptoBits(PrivateKey(scala.io.Codec.toUTF8(privateKey)))
  val time = Clock.systemUTC()
}

object Config extends Loadable[Config] {

  def load(configFile: String): IO[Config] = load(configFile, identity)

}

