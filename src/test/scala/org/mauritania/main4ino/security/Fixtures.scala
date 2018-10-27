package org.mauritania.main4ino.security

import org.mauritania.main4ino.security.Authentication.Token

object Fixtures {

  val ValidToken: Token = "012345678901234567890123456789"
  val User1 = User(1L, "name", "user@zzz.com", List("/"), ValidToken)

}
