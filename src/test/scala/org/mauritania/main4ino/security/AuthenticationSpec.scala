package org.mauritania.main4ino.security

import org.http4s.Uri.Path
import org.http4s.{Header, Headers, Uri}
import org.mauritania.main4ino.security.Authentication.Token
import org.reactormonk.{CryptoBits, PrivateKey}
import org.scalatest.{Matchers, WordSpec}

class AuthenticationSpec extends WordSpec with Matchers {

  val User1 = Fixtures.User1
  def Crypto = CryptoBits(PrivateKey(scala.io.Codec.toUTF8(" " * 20)))

  "The authentication" should {

    "fail when no token is provided in the request" in {
      val h = Headers()
      val u = Uri.unsafeFromString("http://main4ino.com/api/v1/")
      val t = Authentication.tokenFromRequest(Crypto, h, u)
      t shouldBe Left(Authentication.TokenNotProvidedMsg)
    }

    /*
    "retrieve token from cookie" in { // TODO write
    }
    */

    "retrieve token from header as Authorization: token <token>" in {
      val headers = Headers(Header("Authorization", "token " + User1.token))
      val uri = Uri.unsafeFromString("http://main4ino.com/api/v1/device/...")
      val tokenAttempt = Authentication.tokenFromRequest(Crypto, headers, uri)
      tokenAttempt shouldBe Right(User1.token)
    }

    "retrieve token from uri as .../token/<token/..." in {
      val headers = Headers()
      val uri = Uri.unsafeFromString(s"http://main4ino.com/api/v1/token/${User1.token}/device/...")
      val tokenAttempt = Authentication.tokenFromRequest(Crypto, headers, uri)
      tokenAttempt shouldBe Right(User1.token)
    }

    "correctly identify not allowed users to certain uris" in {
      val user = User1.copy(permissionPatterns = List("/api/v1/"))
      val uriPath = "/admin"
      val confUsers = Map(Fixtures.ValidToken -> user)
      val authorizationAttempt = Authentication.authorizedUserFromToken(confUsers, user.token, uriPath)
      authorizationAttempt shouldBe Left(s"User ${user.name} is not authorized to access ${uriPath}")
    }

    "correctly identify allowed users to certain uris" in {
      val user = User1.copy(permissionPatterns = List("/api/v1/"))
      val uriPath = "/api/v1/smth"
      val confUsers = Map(Fixtures.ValidToken -> user)
      val authorizationAttempt = Authentication.authorizedUserFromToken(confUsers, user.token, uriPath)
      authorizationAttempt shouldBe Right(user)
    }

  }
}
