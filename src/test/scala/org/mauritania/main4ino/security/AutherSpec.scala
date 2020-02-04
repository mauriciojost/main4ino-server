package org.mauritania.main4ino.security

import cats.effect.IO
import org.http4s.headers.Authorization
import org.http4s.{AuthedRequest, BasicCredentials, Header, Headers, Method, Request, Uri}
import org.mauritania.main4ino.security.Auther.EncryptionConfig
import org.mauritania.main4ino.security.Fixtures._
import org.mauritania.main4ino.security.MethodRight.RW
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues._
import org.scalatest.wordspec.AnyWordSpec

class AutherSpec extends AnyWordSpec with Matchers {

  val encConfig = EncryptionConfig(PrivateKey.getBytes)
  val User1Token = BasicCredentials(User1.id, User1Pass).token
  val AuthorizationHeaderUser1 = Authorization(BasicCredentials(User1.id, User1Pass))

  "The authentication" should {

    "fail when no token is provided in the request" in {
      val h = Headers.of()
      val u = Uri.unsafeFromString("http://main4ino.com/api/v1/")
      val t = Auther.userCredentialsFromRequest[IO](h, u)
      t shouldBe None
    }

    "retrieve token from header as Authorization: token <token>" in {
      val headers = Headers.of(AuthorizationHeaderUser1)
      val uri = Uri.unsafeFromString("http://main4ino.com/api/v1/device/...")
      val creds = Auther.userCredentialsFromRequest[IO](headers, uri)
      creds shouldBe Some((User1.id, User1Pass))
    }

    "retrieve token from uri as .../token/<token>/..." in {
      val headers = Headers.of()
      val uri = Uri.unsafeFromString(s"http://main4ino.com/api/v1/token/${User1Token}/device/...")
      val creds = Auther.userCredentialsFromRequest[IO](headers, uri)
      creds shouldBe Some((User1.id, User1Pass))
    }

    "retrieve session from uri as .../session/<session>/..." in {
      val headers = Headers.of()
      val uri = Uri.unsafeFromString(s"http://main4ino.com/api/v1/session/session1/device/...")
      val creds = Auther.sessionFromRequest(headers, uri)
      creds shouldBe Some("session1")
    }

    "correctly identify not allowed users to certain uris" in {
      val user = User1.copy(granted = Map[String, MethodRight]("/api/v1/" -> RW))
      val uriPath = "/admin"
      val authorizationAttempt = Auther.checkAccess(user, Method.POST, uriPath)
      authorizationAttempt.isLeft shouldBe(true)
      authorizationAttempt.left.get should include(user.id)
      authorizationAttempt.left.get should include(uriPath)
    }

    "correctly identify allowed users to certain uris" in {
      val user = User1.copy(granted = Map[String, MethodRight]("/api/v1/" -> RW))
      val uriPath = "/api/v1/smth"
      val authorizationAttempt = Auther.checkAccess(user, Method.POST, uriPath)
      authorizationAttempt shouldBe Right(user)
    }

    "authenticate and check access (credentials via basic-auth headers)" in {
      val user = userWithRights("/api/")
      val auther = new Auther[IO](configFromUser(user))

      val result1 = auther.authenticateAndCheckAccess(request(s"/api/time", Headers.of(AuthorizationHeaderUser1))).unsafeRunSync()
      result1.right.value.context should be(user)
      result1.right.value.req.uri.path should be("/api/time")

      val badAuthorization = Authorization(BasicCredentials(User1.id, "wrong-password"))
      val result2 = auther.authenticateAndCheckAccess(request(s"/not-allowed/time", Headers.of(badAuthorization))).unsafeRunSync
      result2.left.value should include("Could not authenticate")

      val result3 = auther.authenticateAndCheckAccess(request(s"/not-allowed/time", Headers.of(AuthorizationHeaderUser1))).unsafeRunSync
      result3.left.value should include("not authorized")
    }

    "authenticate and check access (credentials uri token)" in {
      val user = userWithRights("/api/")
      val auther = new Auther[IO](configFromUser(user))

      val result1 = auther.authenticateAndCheckAccess(request(s"/api/token/$User1Token/time")).unsafeRunSync()
      result1.right.value.context should be(user)
      result1.right.value.req.uri.path should be("/api/time")

      val result2 = auther.authenticateAndCheckAccess(request(s"/not-allowed/token/$User1Token/time")).unsafeRunSync
      result2.left.value should include("not authorized")
    }

    "authenticate and check access (credentials via session token)" in {
      val user = userWithRights("/api/")
      val auther = new Auther[IO](configFromUser(user))
      val session = auther.generateSession(user).unsafeRunSync()

      val result1 = auther.authenticateAndCheckAccess(request(s"/api/session/$session/time")).unsafeRunSync()
      result1.right.value.context should be(user)
      result1.right.value.req.uri.path should be("/api/time")

      val result2 = auther.authenticateAndCheckAccess(request(s"/not-allowed/session/$session/time")).unsafeRunSync
      result2.left.value should include("not authorized")
    }

  }

  private def configFromUser(u: User): Config = Config(List(u), "key")
  private def request(uri: String, h: Headers = Headers.empty): Request[IO] = Request[IO](
    method = Method.GET,
    uri = Uri.unsafeFromString(uri),
    headers = h
  )
  private def userWithRights(l: String*): User = User1.copy(granted = l.toList.map(_ -> RW).toMap)

}
