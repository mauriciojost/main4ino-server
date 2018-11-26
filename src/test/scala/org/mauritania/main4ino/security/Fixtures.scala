package org.mauritania.main4ino.security

object Fixtures {

  val Salt = "$2a$10$TxnbkbAGirLy3Rdgt3xPiu"

  val User1Pass = "password"

  val User1 = User(
    name = "name",
    hashedPass = Authentication.passHash(User1Pass, Salt),
    email = "user@zzz.com",
    permissionPatterns = List("/")
  )

}
