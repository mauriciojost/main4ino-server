package org.mauritania.main4ino.security

import cats.data.Kleisli
import cats.effect.IO
import org.http4s.{Request, headers}
import org.http4s.Uri.Path
import org.http4s.headers.Authorization
import org.mauritania.main4ino.security.Authentication.{AuthAttempt, Token}

import scala.util.Try

class Authentication(config: Config) {

  private final val TokenExpr = "[a-zA-Z0-9]{30}"
  private final val HeaderTokenRegex = ("^token (" + TokenExpr + ")$").r
  private final val UriTokenRegex = ("^(.*)/token/(" + TokenExpr + ")/(.*)$").r
  private final val UsersByToken: Map[Token, List[User]] = config.users.groupBy(_.token)

  private def retrieveUser(token: Token, url: Path): AuthAttempt = {
    for {
      u <- UsersByToken.get(token).flatMap(_.headOption).toRight(s"Could not find user for token $token")
      ua <- u.allowed(url).toRight(s"User ${u.name} is not authorized to access ${url}")
    } yield(ua)
  }

  private def retrieveToken(request: Request[IO]): Either[String, Token] = {
    val fromHeader =
      request.headers.get(Authorization).flatMap(v => HeaderTokenRegex.findFirstMatchIn(v.value)).flatMap(a => Try(a.group(1)).toOption)
    val fromUri =
      UriTokenRegex.findFirstMatchIn(request.uri.path).flatMap(a => Try(a.group(2)).toOption)
    val fromCookie =
      headers.Cookie.from(request.headers).flatMap(_.values.toList.find(_.name == "authcookie").map(_.content))
    fromHeader.orElse(fromUri).orElse(fromCookie).toRight("Header 'Authorization' not present, no .../token/<token>/... in uri, no authcookie")
  }

  private def discardToken(path: Path): Path = {
    UriTokenRegex.findFirstMatchIn(path).map(m => m.group(1) + "/" + m.group(3)).getOrElse(path)
  }

  /**
    * Authenticate the user given a request.
    *
    * Implemented as a Kleisli where the request is given, and a effect is
    * obtained as a result, containing the result of the authentication as o
    * [[AuthAttempt]].
    */
  val authUser: Kleisli[IO, Request[IO], AuthAttempt] = Kleisli({ request =>
    IO {
      val user: AuthAttempt = for {
        tkn <- retrieveToken(request)
        user <- retrieveUser(tkn, discardToken(request.uri.path))
      } yield (user)
      user
    }
  })

}

object Authentication {

  type AuthErrorMsg = String
  type AuthAttempt = Either[AuthErrorMsg, User]
  type Token = String

}
