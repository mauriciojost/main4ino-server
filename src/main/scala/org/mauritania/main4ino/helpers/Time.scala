package org.mauritania.main4ino.helpers

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset, ZonedDateTime}

import org.mauritania.main4ino.models.EpochSecTimestamp
import cats.effect.Sync

object Time {
  def asString(dt: ZonedDateTime): String = dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  def asTimestamp(dt: ZonedDateTime): EpochSecTimestamp = dt.toInstant.getEpochSecond // in seconds from epoch
}

class Time[F[_]: Sync] {
  def nowUtc: F[ZonedDateTime] = Sync[F].delay(ZonedDateTime.now(ZoneOffset.UTC))
}

