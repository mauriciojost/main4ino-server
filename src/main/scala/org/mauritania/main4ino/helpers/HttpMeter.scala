package org.mauritania.main4ino.helpers

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.Request
import org.http4s.server.HttpMiddleware

object HttpMeter {

  private def now[F[_]: Sync]: F[Long] = Sync[F].delay(System.currentTimeMillis())

  def timedHttpMiddleware[F[_]: Sync]: HttpMiddleware[F] =
    k =>
      Kleisli { r: Request[F] =>
        for {
          logger <- OptionT.liftF(Slf4jLogger.fromClass[F](getClass))
          start <- OptionT.liftF(now)
          x <- k(r)
          finish <- OptionT.liftF(now)
          diff = finish - start
          msg = s"< ${r.method} ${r.uri} > ${x.status} took ${diff}ms"
          _ <- OptionT.liftF(logger.debug(msg))
        } yield x
      }

}
