package org.mauritania.botinobe.security

import org.scalatest._

class UserSpec extends FlatSpec with Matchers {
  def user(rights: List[String]) = User(1L, "name", "user@zzz.com", rights, "token")

  "The user" should "load correctly a configuration file" in {
    user(List("/")).allowed("/api/v1/").isDefined shouldBe true
    user(List("/api/v2")).allowed("/api/v1/").isDefined shouldBe false
  }

}
