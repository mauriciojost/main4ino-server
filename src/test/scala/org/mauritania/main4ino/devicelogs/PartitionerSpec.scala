package org.mauritania.main4ino.devicelogs

import java.time.{LocalDateTime, ZoneOffset}

import org.mauritania.main4ino.devicelogs.Partitioner.{DayPartitioner, HourPartitioner, MinutePartitioner, EpochSecPartitioner}
import org.scalatest.ParallelTestExecution
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PartitionerSpec extends AnyFlatSpec with Matchers with ParallelTestExecution {

  private def asEpochSec(s: String) = LocalDateTime.parse(s).toEpochSecond(ZoneOffset.UTC)

  "The day partitioner" should "partition at day level" in {
    DayPartitioner.partition(0) should be("1970-01-01")
    DayPartitioner.partition(asEpochSec("2000-01-01T00:00:00")) should be("2000-01-01")
    DayPartitioner.partition(asEpochSec("2000-01-01T23:59:59")) should be("2000-01-01")
    DayPartitioner.partitions(
      asEpochSec("2000-01-01T21:30:00"),
      asEpochSec("2000-01-03T23:59:59")
    ) should be(
      List("2000-01-01", "2000-01-02", "2000-01-03")
    )
  }

  "The hour partitioner" should "partition at hour level" in {
    HourPartitioner.partition(0) should be("1970-01-01-00")
    HourPartitioner.partition(asEpochSec("2000-01-01T00:00:00")) should be("2000-01-01-00")
    HourPartitioner.partition(asEpochSec("2000-01-01T23:59:59")) should be("2000-01-01-23")
    HourPartitioner.partitions(
      asEpochSec("2000-01-01T21:30:00"),
      asEpochSec("2000-01-01T23:59:59")
    ) should be(
      List("2000-01-01-21", "2000-01-01-22", "2000-01-01-23")
    )

    def dt(h: Int, m: Int = 0): Long = LocalDateTime.parse(s"2000-01-01T0$h:0$m:00").atOffset(ZoneOffset.UTC).toEpochSecond
    HourPartitioner.partitions(dt(1, 1), dt(3)) should be(
      List("2000-01-01-01", "2000-01-01-02", "2000-01-01-03")
    )
  }

  "The minute partitioner" should "partition at minutes level" in {
    MinutePartitioner.partition(0) should be("1970-01-01-00-00")
    MinutePartitioner.partition(asEpochSec("2000-01-01T00:00:00")) should be("2000-01-01-00-00")
    MinutePartitioner.partition(asEpochSec("2000-01-01T23:59:59")) should be("2000-01-01-23-59")
    MinutePartitioner.partitions(
      asEpochSec("2000-01-01T21:30:00"),
      asEpochSec("2000-01-01T21:33:59")
    ) should be(
      List("2000-01-01-21-30", "2000-01-01-21-31","2000-01-01-21-32","2000-01-01-21-33")
    )
  }

  "The epoch seconds partitioner" should "partition at seconds level" in {
    EpochSecPartitioner.partition(0) should be("0")
    EpochSecPartitioner.partition(1) should be("1")
    EpochSecPartitioner.partitions(0, 1) should be(List("0", "1"))
  }

}
