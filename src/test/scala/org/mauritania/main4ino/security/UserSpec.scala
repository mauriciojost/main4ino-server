package org.mauritania.main4ino.security

import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UserSpec extends AnyFlatSpec with Matchers {
  def user(rights: List[String]) = Fixtures.User1.copy(granted = rights)

  "The user" should "load correctly a configuration file" in {
    user(List("/")).authorized("/api/v1/").isDefined shouldBe true
    user(List("/api/v2")).authorized("/api/v1/").isDefined shouldBe false
  }

}
