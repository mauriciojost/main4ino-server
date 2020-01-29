package org.mauritania.main4ino.security

import org.http4s.Method
import org.mauritania.main4ino.security.MethodRight.{MethodRight, R, RW, W}
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UserSpec extends AnyFlatSpec with Matchers {
  def user(rights: List[String], methodRight: MethodRight) = Fixtures.User1.copy(granted = rights.map(_ -> methodRight).toMap)

  "The user" should "load correctly a configuration file" in {
    user(List("/"), RW).authorized(Method.GET, "/api/v1/").isDefined shouldBe true
    user(List("/api/v2"), RW).authorized(Method.GET, "/api/v1/").isDefined shouldBe false

    user(List("/"), R).authorized(Method.GET, "/api/v1/").isDefined shouldBe true
    user(List("/"), W).authorized(Method.GET, "/api/v1/").isDefined shouldBe false

    user(List("/"), RW).authorized(Method.GET, "/api/v1/").isDefined shouldBe true
    user(List("/"), RW).authorized(Method.POST, "/api/v1/").isDefined shouldBe true
    user(List("/"), RW).authorized(Method.PUT, "/api/v1/").isDefined shouldBe true
    user(List("/"), RW).authorized(Method.DELETE, "/api/v1/").isDefined shouldBe true

    user(List("/"), R).authorized(Method.GET, "/api/v1/").isDefined shouldBe true
    user(List("/"), R).authorized(Method.POST, "/api/v1/").isDefined shouldBe false
    user(List("/"), R).authorized(Method.PUT, "/api/v1/").isDefined shouldBe false
    user(List("/"), R).authorized(Method.DELETE, "/api/v1/").isDefined shouldBe false

    user(List("/"), W).authorized(Method.GET, "/api/v1/").isDefined shouldBe false
    user(List("/"), W).authorized(Method.POST, "/api/v1/").isDefined shouldBe true
    user(List("/"), W).authorized(Method.PUT, "/api/v1/").isDefined shouldBe true
    user(List("/"), W).authorized(Method.DELETE, "/api/v1/").isDefined shouldBe true
  }

}
