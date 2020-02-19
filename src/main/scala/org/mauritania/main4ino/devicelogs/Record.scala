package org.mauritania.main4ino.devicelogs

import java.time.Instant

import org.mauritania.main4ino.models.EpochSecTimestamp

case class Record(
  t: EpochSecTimestamp,
  content: String
)

object Record {
  final val LogRegex = "^(\\d+) (.*)$".r
  def parse(s: String): Option[Record] = {
    s match {
      case LogRegex(t, m) => Some(Record(t.toLong, m))
      case _ => None
    }
  }
}
