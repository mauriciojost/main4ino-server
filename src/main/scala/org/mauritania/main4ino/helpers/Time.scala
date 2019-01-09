package org.mauritania.main4ino.helpers

import java.time.format.DateTimeFormatter
import java.util.Date
import java.time.{ZoneOffset, ZonedDateTime}

import org.mauritania.main4ino.models.Timestamp
import cats.effect.IO

trait Time[F[_]] {
  def nowUtc: F[ZonedDateTime]
}

object Time {
  def asString(t: Timestamp): String = new Date(t).toString
  def asString(dt: ZonedDateTime): String = dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  def asTimestamp(dt: ZonedDateTime): Timestamp = dt.toInstant.getEpochSecond // in seconds from epoch
}

class TimeIO extends Time[IO] {
  def nowUtc: IO[ZonedDateTime] = IO(ZonedDateTime.now(ZoneOffset.UTC))
}

