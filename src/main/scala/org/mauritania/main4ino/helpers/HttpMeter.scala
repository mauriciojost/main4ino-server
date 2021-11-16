package org.mauritania.main4ino.helpers

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.http4s.{Request, Uri}
import org.http4s.server.HttpMiddleware

object HttpMeter {

  private final val PathSeparator = "/"
  private final val ShortenFrom = 0
  private final val ShortenTo = 15

  private def now[F[_]: Sync]: F[Long] = Sync[F].delay(System.currentTimeMillis())

  private def shorten(uri: Uri): Uri = {
    val newPath =
      uri.path.split(PathSeparator).map(_.slice(ShortenFrom, ShortenTo)).mkString(PathSeparator)
    uri.copy(path = newPath)
  }

  def timedHttpMiddleware[F[_]: Sync]: HttpMiddleware[F] =
    k =>
      Kleisli { r: Request[F] =>
        for {
          logger <- OptionT.liftF(Slf4jLogger.fromClass[F](getClass))
          start <- OptionT.liftF(now)
          x <- k(r)
          finish <- OptionT.liftF(now)
          diff = finish - start
          msgDebug = s"< ${r.method} ${shorten(r.uri)} > ${x.status} took ${diff}ms"
          msgTrace = s"< ${r.method} ${r.uri} ${r.headers} > ${x.status} took ${diff}ms"
          _ <- OptionT.liftF(logger.debug(msgDebug))
          _ <- OptionT.liftF(logger.trace(msgTrace))
        } yield x
      }

}
