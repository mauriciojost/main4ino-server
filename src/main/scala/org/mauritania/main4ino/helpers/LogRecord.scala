package org.mauritania.main4ino.helpers

import org.mauritania.main4ino.models.EpochSecTimestamp

case class LogRecord(
  t: EpochSecTimestamp,
  content: String
) {
  def pretty: String = {
    s"$t $content"
  }
}

object LogRecord {
  final val LogRegex = "^(\\d+) (.*)$".r
  def parse(s: String): Option[LogRecord] = {
    s match {
      case LogRegex(t, m) => Some(LogRecord(t.toLong, m))
      case _ => None
    }
  }
}



