package org.mauritania.main4ino.helpers

import cats.effect.Sync
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.mauritania.main4ino.security.Auther.AccessAttempt
import cats.implicits._

object AuthLogger {

  def logAuthentication[F[_]: Sync](user: AccessAttempt): F[AccessAttempt] = {
    for {
      logger <- Slf4jLogger.fromClass[F](getClass)
      msg = user match {
        case Right(i) => s">>> Authenticated: ${i.name} ${i.email}"
        case Left(m) => s">>> Failed to authenticate: $m"
      }
      _ <- logger.debug(msg)
    } yield (user)
  }

}
