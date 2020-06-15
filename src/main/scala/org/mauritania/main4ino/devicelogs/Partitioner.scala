package org.mauritania.main4ino.devicelogs

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}

import org.mauritania.main4ino.models.EpochSecTimestamp

object Partitioner {

  type RecordId = EpochSecTimestamp
  type Partition = String


  sealed trait Partitioner {
    def partition(r: RecordId): Partition
  }

  case object EpochSecPartitioner extends Partitioner {
    def partition(r: RecordId): Partition = r.toString
  }

  case object HourPartitioner extends Partitioner {
    private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH")
    def partition(r: RecordId): Partition = Instant.ofEpochSecond(r).atZone(ZoneOffset.UTC).format(format)
  }

  case object DayPartitioner extends Partitioner {
    private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    def partition(r: RecordId): Partition = Instant.ofEpochSecond(r).atZone(ZoneOffset.UTC).format(format)
  }

}
