package org.mauritania.main4ino.devicelogs

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}

import org.mauritania.main4ino.models.EpochSecTimestamp

object Partitioner {

  type Partition = String

  def rangeComplete(f: EpochSecTimestamp, t: EpochSecTimestamp)(step: EpochSecTimestamp): Set[EpochSecTimestamp] = ((f.to(t, step)) :+ t).toSet

  sealed trait Partitioner {
    def partition(r: EpochSecTimestamp): Partition
    def partitions(f: EpochSecTimestamp, t: EpochSecTimestamp): List[Partition]
  }

  case object EpochSecPartitioner extends Partitioner {
    def partition(r: EpochSecTimestamp): Partition = r.toString
    def partitions(f: EpochSecTimestamp, t: EpochSecTimestamp): List[Partition] = (f to t).toList.map(partition)
  }

  case object HourPartitioner extends Partitioner {
    private final val SecsInHour = 60 * 60
    private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH")
    def partition(r: EpochSecTimestamp): Partition = Instant.ofEpochSecond(r).atZone(ZoneOffset.UTC).format(format)
    def partitions(f: EpochSecTimestamp, t: EpochSecTimestamp): List[Partition] = rangeComplete(f, t)(SecsInHour).map(partition).toList.sorted
  }

  case object DayPartitioner extends Partitioner {
    private final val SecsInDay = 60 * 60 * 24
    private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    def partition(r: EpochSecTimestamp): Partition = Instant.ofEpochSecond(r).atZone(ZoneOffset.UTC).format(format)
    def partitions(f: EpochSecTimestamp, t: EpochSecTimestamp): List[Partition] = rangeComplete(f, t)(SecsInDay).map(partition).toList.sorted
  }

}
