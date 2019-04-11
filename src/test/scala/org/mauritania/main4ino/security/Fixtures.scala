package org.mauritania.main4ino.security

object Fixtures {

  val Salt = "$2a$10$TxnbkbAGirLy3Rdgt3xPiu"

  val User1Pass = "password"

  val User1 = User(
    name = "name",
    hashedpass = Auther.hashPassword(User1Pass, Salt),
    email = "user@zzz.com",
    granted = List("/")
  )

  val PrivateKey = "0123456789abcdef0123"
  val DefaultSecurityConfig = Config(List(User1), PrivateKey, Salt)

}
