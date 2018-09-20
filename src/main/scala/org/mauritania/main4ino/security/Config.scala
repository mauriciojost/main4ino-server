package org.mauritania.main4ino.security

import cats.effect.IO
import org.mauritania.main4ino.config.Loadable

case class Config(
  users: List[User]
) {
  def deduplicateTokens: Config = {
    val byToken = users.groupBy(_.token)
    val withSingleUser = byToken.collect {
      case (_, users) if users.length == 1 => users.head
    }
    Config(withSingleUser.toList)
  }
}

object Config extends Loadable[Config] {

  def load(configFile: String): IO[Config] = load(configFile, _.deduplicateTokens)

}

