package org.mauritania.main4ino.devicelogs

import java.time.{LocalDateTime, ZoneOffset}

import org.mauritania.main4ino.devicelogs.Partitioner.{DayPartitioner, HourPartitioner, EpochSecPartitioner}
import org.scalatest.ParallelTestExecution
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PartitionerSpec extends AnyFlatSpec with Matchers with ParallelTestExecution {

  private def asEpochSec(s: String) = LocalDateTime.parse(s).toEpochSecond(ZoneOffset.UTC)

  "The day partitioner" should "partition at day level" in {
    DayPartitioner.partition(0) should be("1970-01-01")
    DayPartitioner.partition(asEpochSec("2000-01-01T00:00:00")) should be("2000-01-01")
    DayPartitioner.partition(asEpochSec("2000-01-01T23:59:59")) should be("2000-01-01")
  }

  "The hour partitioner" should "partition at day level" in {
    HourPartitioner.partition(0) should be("1970-01-01-00")
    HourPartitioner.partition(asEpochSec("2000-01-01T00:00:00")) should be("2000-01-01-00")
    HourPartitioner.partition(asEpochSec("2000-01-01T23:59:59")) should be("2000-01-01-23")
  }

  "The epoch seconds partitioner" should "partition at day level" in {
    EpochSecPartitioner.partition(0) should be("0")
    EpochSecPartitioner.partition(1) should be("1")
  }

}
