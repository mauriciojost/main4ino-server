package org.mauritania.main4ino

import org.http4s.{Headers, Uri}
import org.mauritania.main4ino.security.Authentication
import org.mauritania.main4ino.security.Authentication.Token
import org.scalatest.{Matchers, WordSpec}

class AuthenticationSpec extends WordSpec with Matchers {

  val ValidToken: Token = "012345678901234567890123456789"

  "The authentication" should {

    "reject tokens that do not belong to any user" in {
      val h = Headers()
      val u = Uri()
      val t = Authentication.tokenFromRequest(h, u)
      t shouldBe Left(Authentication.InvalidTokenMsg)
    }

  }
}
