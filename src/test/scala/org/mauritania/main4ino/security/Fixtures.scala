package org.mauritania.main4ino.security

import org.mauritania.main4ino.security.MethodRight.RW
import cats.effect.IO

object Fixtures {

  val Salt = "$2a$10$TxnbkbAGirLy3Rdgt3xPiu"

  val User1Pass = "password"

  val User1 = User(
    name = "name",
    hashedpass = Auther.hashPassword[IO](User1Pass, Salt).unsafeRunSync(),
    email = "user@zzz.com",
    granted = Map[String, MethodRight]("/" -> RW)
  )

  val PrivateKey = "0123456789abcdef0123"
  val DefaultSecurityConfig = Config(List(User1), PrivateKey, Salt)

}
