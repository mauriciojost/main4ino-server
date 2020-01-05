package org.mauritania.main4ino.helpers

import cats.{Applicative, Monad}
import cats.data.{Kleisli, OptionT}
import org.http4s.{AuthedRequest, AuthedRoutes, Request, Response, Status}
import org.http4s.server.AuthMiddleware
import cats.implicits._

/**
  * Custom Authentication Middleware that allows to intercept
  * the request being passed in case of successful authentication.
  */
object CustomAuthMiddleware {

  def apply[F[_], Err, T](
    authUser: Kleisli[F, Request[F], Either[Err, AuthedRequest[F, T]]],
    onFailure: AuthedRoutes[Err, F]
  )(implicit F: Monad[F]): AuthMiddleware[F, T] =
    (routes: AuthedRoutes[T, F]) =>
      Kleisli { req: Request[F] =>
        OptionT {
          authUser(req).flatMap {
            case Left(err) => onFailure(AuthedRequest(err, req)).value
            case Right(authedRequest) => routes(authedRequest).value
          }
        }
      }
}

