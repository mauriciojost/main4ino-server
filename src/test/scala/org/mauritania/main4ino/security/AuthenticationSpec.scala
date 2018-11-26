package org.mauritania.main4ino.security

import org.http4s.{Header, Headers, Uri}
import org.mauritania.main4ino.security.Authentication.EncryptionConfig
import org.reactormonk.{CryptoBits, PrivateKey}
import org.scalatest.{Matchers, WordSpec}

class AuthenticationSpec extends WordSpec with Matchers {

  val User1 = Fixtures.User1
  val Salt = Fixtures.Salt
  def Crypto = CryptoBits(PrivateKey(scala.io.Codec.toUTF8(" " * 20)))
  val encConfig = EncryptionConfig(Crypto, Salt)

  "The authentication" should {

    "fail when no token is provided in the request" in {
      val h = Headers()
      val u = Uri.unsafeFromString("http://main4ino.com/api/v1/")
      val t = Authentication.userCredentialsFromCredentialsRequest(encConfig, h, u)
      t shouldBe None
    }

    /*
    "retrieve token from header as Authorization: token <token>" in {
      val headers = Headers(Header("Authorization", "token " + User1.token))
      val uri = Uri.unsafeFromString("http://main4ino.com/api/v1/device/...")
      val tokenAttempt = Authentication.userCredentialsFromCredentialsRequest(Crypto, headers, uri)
      tokenAttempt shouldBe Some(User1.token)
    }

    "retrieve token from uri as .../token/<token/..." in {
      val headers = Headers()
      val uri = Uri.unsafeFromString(s"http://main4ino.com/api/v1/token/${User1.token}/device/...")
      val tokenAttempt = Authentication.userCredentialsFromCredentialsRequest(Crypto, headers, uri)
      tokenAttempt shouldBe Some(User1.token)
    }

    "correctly identify not allowed users to certain uris" in {
      val user = User1.copy(permissionPatterns = List("/api/v1/"))
      val uriPath = "/admin"
      val authorizationAttempt = Authentication.authorizedUserFromToken(Right(user), uriPath)
      authorizationAttempt shouldBe Left(s"User ${user.name} is not authorized to access ${uriPath}")
    }

    "correctly identify allowed users to certain uris" in {
      val user = User1.copy(permissionPatterns = List("/api/v1/"))
      val uriPath = "/api/v1/smth"
      val authorizationAttempt = Authentication.authorizedUserFromToken(Right(user), uriPath)
      authorizationAttempt shouldBe Right(user)
    }
    */

  }
}
