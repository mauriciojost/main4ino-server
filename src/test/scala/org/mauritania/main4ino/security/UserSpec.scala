package org.mauritania.main4ino.security

import org.scalatest._

class UserSpec extends FlatSpec with Matchers {
  def user(rights: List[String]) = Fixtures.User1.copy(granted = rights)

  "The user" should "load correctly a configuration file" in {
    user(List("/")).authorized("/api/v1/").isDefined shouldBe true
    user(List("/api/v2")).authorized("/api/v1/").isDefined shouldBe false
  }

}
