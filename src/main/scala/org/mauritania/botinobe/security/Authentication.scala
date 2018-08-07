package org.mauritania.botinobe.security

import cats.data.Kleisli
import cats.effect.IO
import cats.syntax.EitherObjectOps
import org.http4s.Request
import org.http4s.headers.Authorization
import cats.syntax.either._

object Authentication {

  val TokenKeyword = "token"
  val UniqueValidToken = "1122334455" // TODO: fix this hardcoded token

  case class User(id: Long, name: String)

  def toUser(id: Long): User = User(id, "Batman")

  def validate(token: String): Option[String] = {
    token match {
      case v if v == TokenKeyword + " " + UniqueValidToken => Some(UniqueValidToken)
      case _ => None
    }
  }

  def retrieveUser: Kleisli[IO, Long, User] = Kleisli(id => IO(Authentication.toUser(id)))

  val authUser: Kleisli[IO, Request[IO], Either[String, User]] = Kleisli({ request =>
    val message = for {
      header <- request.headers.get(Authorization).toRight("Couldn't find an Authorization header")
      token <- Authentication.validate(header.value).toRight("Invalid token")
      msg <- new EitherObjectOps(Either).catchOnly[NumberFormatException](token.toLong).leftMap(_.toString)
    } yield(msg)
    message.traverse(retrieveUser.run)
  })

}
