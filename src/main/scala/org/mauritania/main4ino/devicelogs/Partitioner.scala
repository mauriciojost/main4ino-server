package org.mauritania.main4ino.devicelogs

import java.time.format.DateTimeFormatter
import java.time.{Duration, Instant, ZoneOffset}

import org.mauritania.main4ino.devicelogs.Partitioner.EpochSecPartitioner.partition
import org.mauritania.main4ino.devicelogs.Partitioner.HourPartitioner.{SecsInHour, partition}
import org.mauritania.main4ino.models.EpochSecTimestamp

object Partitioner {

  type RecordId = EpochSecTimestamp
  type Partition = String


  sealed trait Partitioner {
    def partition(r: RecordId): Partition
    def partitions(f: RecordId, t: RecordId): Set[Partition]
  }

  case object EpochSecPartitioner extends Partitioner {
    def partition(r: RecordId): Partition = r.toString
    def partitions(f: RecordId, t: RecordId): Set[Partition] = (f to t).map(partition).toSet
  }

  case object HourPartitioner extends Partitioner {
    private final val SecsInHour = 60 * 60
    private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH")
    def partition(r: RecordId): Partition = Instant.ofEpochSecond(r).atZone(ZoneOffset.UTC).format(format)
    def partitions(f: RecordId, t: RecordId): Set[Partition] = (f.to(t, SecsInHour)).map(partition).toSet
  }

  case object DayPartitioner extends Partitioner {
    private final val SecsInDay = 60 * 60 * 24
    private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    def partition(r: RecordId): Partition = Instant.ofEpochSecond(r).atZone(ZoneOffset.UTC).format(format)
    def partitions(f: RecordId, t: RecordId): Set[Partition] = (f.to(t, SecsInDay)).map(partition).toSet
  }

}
