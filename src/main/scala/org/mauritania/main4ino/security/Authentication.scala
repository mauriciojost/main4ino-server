package org.mauritania.main4ino.security

import java.time.Clock

import cats.effect.IO
import org.http4s.{Headers, Request, Uri, headers => http4sHeaders}
import org.http4s.Uri.Path
import org.http4s.headers.Authorization
import org.mauritania.main4ino.security.Authentication.{AuthAttempt, Session, SessionAttempt}
import org.reactormonk.CryptoBits

import scala.util.Try

trait Authentication[F[_]] {

  /**
    * Authenticate the user given a request.
    *
    * Implemented as a Kleisli where the request is given, and a effect is
    * obtained as a result, containing the result of the authentication as a
    * [[AuthAttempt]].
    * It should interpret:
    * - token from uri
    * - token from headers
    * - token from cookies
    * - session from cookies
    */
  def authenticateUser(request: Request[F]): F[AuthAttempt]

  /**
    * Provide a session given a user
    */
  def sessionUser(user: User): F[Session]

}

class AuthenticationIO(config: Config) extends Authentication[IO] {
  val UsersByToken = config.usersByToken
  def authenticateUser(request: Request[IO]): IO[AuthAttempt] =
    IO.pure(Authentication.authorizedUserFromRequest(UsersByToken, config.privateKeyBits, request.headers, request.uri))
  def sessionUser(user: User): IO[Session] =
    IO.pure(Authentication.sessionFromUser(user, config.privateKeyBits, config.time))
}

object Authentication {

  type Token = String
  type Session = String
  type AuthErrorMsg = String
  type LoginErrorMsg = String
  type AuthAttempt = Either[AuthErrorMsg, User]
  type SessionAttempt = Either[LoginErrorMsg, Session]

  final val AuthCookieName = "authcookie"
  final val AuthCookieSessionName = "authcookiesession"
  final val TokenExpr = "[a-zA-Z0-9]{30}"
  private final val HeaderTokenRegex = ("^token (" + TokenExpr + ")$").r
  private final val UriTokenRegex = ("^(.*)/token/(" + TokenExpr + ")/(.*)$").r
  private [security] final val TokenNotProvidedMsg = "Header 'Authorization' not present, no .../token/<token>/... in uri, no authcookie"

  def authorizedUserFromRequest(usersByToken: Map[Token, User], privateKey: CryptoBits, headers: Headers, uri: Uri): AuthAttempt = {
    val user: AuthAttempt = for {
      tkn <- tokenFromRequest(privateKey, headers, uri)
      user <- authorizedUserFromToken(usersByToken, tkn, discardToken(uri.path))
    } yield (user)
    user
  }

  /**
    * Create a session for a given user given a private key and a timestamp
    * @param user
    * @param privateKey
    * @param time
    * @return
    */
  def sessionFromUser(user: User, privateKey: CryptoBits, time: Clock): Session =
    privateKey.signToken(user.email, time.millis.toString())

  def authorizedUserFromToken(usersByToken: Map[Token, User], token: Token, uriPath: Path): AuthAttempt = {
    for {
      u <- usersByToken.get(token).toRight(s"Could not find user for token $token")
      ua <- u.authorized(uriPath).toRight(s"User ${u.name} is not authorized to access ${uriPath}")
    } yield(ua)
  }

  def tokenFromRequest(privateKey: CryptoBits, headers: Headers, uri: Uri): Either[String, Token] = {
    val fromHeader =
      headers.get(Authorization).flatMap(v => HeaderTokenRegex.findFirstMatchIn(v.value)).flatMap(a => Try(a.group(1)).toOption)
    val fromUri =
      UriTokenRegex.findFirstMatchIn(uri.path).flatMap(a => Try(a.group(2)).toOption)
    val fromCookie =
      http4sHeaders.Cookie.from(headers).flatMap(_.values.toList.find(_.name == AuthCookieName).map(_.content))
    val fromSessionCookie =
      http4sHeaders.Cookie.from(headers).flatMap(_.values.toList.find(_.name == AuthCookieSessionName).map(_.content)).flatMap(privateKey.validateSignedToken)
    fromHeader.orElse(fromUri).orElse(fromCookie).orElse(fromSessionCookie).toRight(TokenNotProvidedMsg)
  }

  private def discardToken(path: Path): Path = {
    UriTokenRegex.findFirstMatchIn(path).map(m => m.group(1) + "/" + m.group(3)).getOrElse(path)
  }

}
