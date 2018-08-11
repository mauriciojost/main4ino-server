package org.mauritania.botinobe.security

import cats.data.Kleisli
import cats.effect.IO
import org.http4s.Request
import org.http4s.headers.Authorization

import scala.util.Try

class Authentication(config: Config) {

  private final val TokenRegex = "^token ([a-zA-Z0-9]{10})$".r // TODO increase to 20 or 30

  lazy val UsersByToken = config.users.groupBy(_.token)

  def retrieveUser(token: String, url: String): Either[String, User] = {
    for {
      u <- UsersByToken.get(token).flatMap(_.headOption).toRight(s"Could not find user for token $token")
      ua <- u.canAccess(url).toRight(s"User ${u.name} is not authorized to access ${url}")
    } yield(ua)
  }

  private def retrieveToken(v: String): Option[String] = {
    val m = TokenRegex.findFirstMatchIn(v)
    m.flatMap(i => Try(i.group(1)).toOption)
  }

  val authUser: Kleisli[IO, Request[IO], Either[String, User]] = Kleisli({ request =>
    IO {
      val u = for {
        header <- request.headers.get(Authorization).toRight("Header 'Authorization' not present")
        tkn <- retrieveToken(header.value).toRight(s"Invalid token syntax: ${header.value}")
        user <- retrieveUser(tkn, request.pathInfo)
      } yield (user)
      u
    }
  })

}
