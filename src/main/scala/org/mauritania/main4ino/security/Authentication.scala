package org.mauritania.main4ino.security

import cats.data.Kleisli
import cats.effect.IO
import org.http4s.Request
import org.http4s.headers.Authorization
import org.mauritania.main4ino.security.Authentication.Token

import scala.util.Try

class Authentication(config: Config) {

  private final val HeaderTokenRegex = ("^token (.*)$").r
  private final val UriTokenRegex = ("^/token/(.*)/(.*)").r
  private final val TokenRegex = "([a-zA-Z0-9]{30})".r

  private lazy val UsersByToken: Map[Token, List[User]] = config.users.groupBy(_.token)

  private def retrieveUser(token: Token, url: String): Either[String, User] = {
    for {
      u <- UsersByToken.get(token).flatMap(_.headOption).toRight(s"Could not find user for token $token")
      ua <- u.allowed(url).toRight(s"User ${u.name} is not authorized to access ${url}")
    } yield(ua)
  }

  private def retrieveToken(v: String): Option[Token] = {
    val m = TokenRegex.findFirstMatchIn(v)
    m.flatMap(i => Try(i.group(1)).toOption)
  }

  private def getToken(request: Request[IO]) = {
    val fromHeader = request.headers.get(Authorization).flatMap(v => HeaderTokenRegex.findFirstIn(v.value))
    val fromUri = UriTokenRegex.findFirstIn(request.uri.path)
    fromHeader.orElse(fromUri).toRight("Header 'Authorization' not present and no .../token/<token>/... in uri")
  }

  def withoutToken(pathInfo: String): String = {
    UriTokenRegex.findFirstMatchIn(pathInfo) match {
      case Some(m) => "/" + m.group(2)
      case None => pathInfo
    }
  }

  val authUser: Kleisli[IO, Request[IO], Either[String, User]] = Kleisli({ request =>
    IO {
      val u = for {
        tknStr <- getToken(request)
        tkn <- retrieveToken(tknStr).toRight(s"Invalid token syntax")
        user <- retrieveUser(tkn, withoutToken(request.pathInfo))
      } yield (user)
      u
    }
  })

}

object Authentication {

  type Token = String

}
