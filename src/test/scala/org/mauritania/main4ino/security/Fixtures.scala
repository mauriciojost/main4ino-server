package org.mauritania.main4ino.security

import org.mauritania.main4ino.security.Permission.RW
import cats.effect.IO

object Fixtures {

  val User1Pass = "password"

  val User1 = User(
    name = "name",
    hashedpass = Auther.hashPassword[IO](User1Pass).unsafeRunSync(),
    email = "user@zzz.com",
    granted = Map[String, Permission]("/" -> RW)
  )

  val PrivateKey = "0123456789abcdef0123".getBytes()
  val DefaultSecurityConfig = Config(List(User1), PrivateKey)

}
