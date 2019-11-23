package org.mauritania.main4ino.helpers

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset, ZonedDateTime}

import org.mauritania.main4ino.models.EpochSecTimestamp
import cats.effect.Sync

trait Time[F[_]] {
  def nowUtc: F[ZonedDateTime]
}

object Time {
  def asString(dt: ZonedDateTime): String = dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  def asTimestamp(dt: ZonedDateTime): EpochSecTimestamp = dt.toInstant.getEpochSecond // in seconds from epoch
}

class TimeIO[F[_]: Sync] extends Time[F] {
  def nowUtc: F[ZonedDateTime] = Sync[F].delay(ZonedDateTime.now(ZoneOffset.UTC))
}

