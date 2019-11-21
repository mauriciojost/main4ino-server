package org.mauritania.main4ino.helpers

import cats.effect.Sync
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.mauritania.main4ino.security.Auther.AccessAttempt
import cats.implicits._

object AuthLogger {

  def logAuthentication[F[_]: Sync](user: AccessAttempt): F[AccessAttempt] = {
    for {
      logger <- Slf4jLogger.fromClass[F](getClass)
      _ <- user match {
        case Right(i) => logger.debug(s">>> Authenticated: ${i.name}/${i.email}")
        case Left(m) => logger.info(s">>> Failed to authenticate: $m")
      }
    } yield (user)
  }

}
