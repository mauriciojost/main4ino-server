package org.mauritania.main4ino.security

import cats.effect.IO
import org.http4s.{Headers, Request, Uri, headers => http4sHeaders}
import org.http4s.Uri.Path
import org.http4s.headers.Authorization
import org.mauritania.main4ino.security.Authentication.{AuthAttempt, Token}

import scala.util.Try

trait Authentication[F[_]] {

  /**
    * Authenticate the user given a request.
    *
    * Implemented as a Kleisli where the request is given, and a effect is
    * obtained as a result, containing the result of the authentication as o
    * [[AuthAttempt]].
    */
  def authenticateUser(request: Request[F]): F[AuthAttempt]

}

class AuthenticationIO(config: Config) extends Authentication[IO] {
  val UsersByToken = config.users.groupBy(_.token)
  def authenticateUser(request: Request[IO]): IO[AuthAttempt] =
    IO.pure(Authentication.authorizedUserFromRequest(UsersByToken, request.headers, request.uri))
}

object Authentication {

  type AuthErrorMsg = String
  type AuthAttempt = Either[AuthErrorMsg, User]
  type Token = String

  private final val AuthCookieName = "authcookie"
  private final val TokenExpr = "[a-zA-Z0-9]{30}"
  private final val HeaderTokenRegex = ("^token (" + TokenExpr + ")$").r
  private final val UriTokenRegex = ("^(.*)/token/(" + TokenExpr + ")/(.*)$").r
  private [security] final val TokenNotProvidedMsg = "Header 'Authorization' not present, no .../token/<token>/... in uri, no authcookie"

  def authorizedUserFromRequest(usersByToken: Map[Token, List[User]], headers: Headers, uri: Uri): AuthAttempt = {
    val user: AuthAttempt = for {
      tkn <- tokenFromRequest(headers, uri)
      user <- authorizedUserFromToken(usersByToken, tkn, discardToken(uri.path))
    } yield (user)
    user
  }

  def authorizedUserFromToken(usersByToken: Map[Token, List[User]], token: Token, uriPath: Path): AuthAttempt = {
    for {
      u <- usersByToken.get(token).flatMap(_.headOption).toRight(s"Could not find user for token $token")
      ua <- u.authorized(uriPath).toRight(s"User ${u.name} is not authorized to access ${uriPath}")
    } yield(ua)
  }

  def tokenFromRequest(headers: Headers, uri: Uri): Either[String, Token] = {
    val fromHeader =
      headers.get(Authorization).flatMap(v => HeaderTokenRegex.findFirstMatchIn(v.value)).flatMap(a => Try(a.group(1)).toOption)
    val fromUri =
      UriTokenRegex.findFirstMatchIn(uri.path).flatMap(a => Try(a.group(2)).toOption)
    val fromCookie =
      http4sHeaders.Cookie.from(headers).flatMap(_.values.toList.find(_.name == AuthCookieName).map(_.content))
    fromHeader.orElse(fromUri).orElse(fromCookie).toRight(TokenNotProvidedMsg)
  }

  def discardToken(path: Path): Path = {
    UriTokenRegex.findFirstMatchIn(path).map(m => m.group(1) + "/" + m.group(3)).getOrElse(path)
  }

}
