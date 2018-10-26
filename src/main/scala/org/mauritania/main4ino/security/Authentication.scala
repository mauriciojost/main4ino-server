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
    IO.pure(Authentication.userFromRequest(UsersByToken, request.headers, request.uri))
}

object Authentication {

  type AuthErrorMsg = String
  type AuthAttempt = Either[AuthErrorMsg, User]
  type Token = String

  private final val TokenExpr = "[a-zA-Z0-9]{30}"
  private final val HeaderTokenRegex = ("^token (" + TokenExpr + ")$").r
  private final val UriTokenRegex = ("^(.*)/token/(" + TokenExpr + ")/(.*)$").r
  final val InvalidTokenMsg = "Header 'Authorization' not present, no .../token/<token>/... in uri, no authcookie"

  def userFromRequest(usersByToken: Map[Token, List[User]], headers: Headers, uri: Uri): AuthAttempt = {
    val user: AuthAttempt = for {
      tkn <- tokenFromRequest(headers, uri)
      user <- userFromToken(usersByToken, tkn, discardToken(uri.path))
    } yield (user)
    user
  }

  def userFromToken(usersByToken: Map[Token, List[User]], token: Token, url: Path): AuthAttempt = {
    for {
      u <- usersByToken.get(token).flatMap(_.headOption).toRight(s"Could not find user for token $token")
      ua <- u.allowed(url).toRight(s"User ${u.name} is not authorized to access ${url}")
    } yield(ua)
  }

  def tokenFromRequest(headers: Headers, uri: Uri): Either[String, Token] = {
    val fromHeader =
      headers.get(Authorization).flatMap(v => HeaderTokenRegex.findFirstMatchIn(v.value)).flatMap(a => Try(a.group(1)).toOption)
    val fromUri =
      UriTokenRegex.findFirstMatchIn(uri.path).flatMap(a => Try(a.group(2)).toOption)
    val fromCookie =
      http4sHeaders.Cookie.from(headers).flatMap(_.values.toList.find(_.name == "authcookie").map(_.content))
    fromHeader.orElse(fromUri).orElse(fromCookie).toRight(InvalidTokenMsg)
  }

  def discardToken(path: Path): Path = {
    UriTokenRegex.findFirstMatchIn(path).map(m => m.group(1) + "/" + m.group(3)).getOrElse(path)
  }

}
