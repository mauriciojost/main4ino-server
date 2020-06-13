package org.mauritania.main4ino.devicelogs

import java.time.{LocalDateTime, ZoneOffset}

import org.mauritania.main4ino.devicelogs.Partitioner.{DayPartitioner, HourPartitioner, IdentityPartitioner}
import org.scalatest.ParallelTestExecution
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PartitionerSpec extends AnyFlatSpec with Matchers with ParallelTestExecution {

  "The day partitioner" should "partition at day level" in {
    def e(s: String) = LocalDateTime.parse(s).toEpochSecond(ZoneOffset.UTC)
    DayPartitioner.partition(0) should be("1970-01-01")
    DayPartitioner.partition(e("2000-01-01T00:00:00")) should be("2000-01-01")
    DayPartitioner.partition(e("2000-01-01T23:59:59")) should be("2000-01-01")
  }

  "The hour partitioner" should "partition at day level" in {
    def e(s: String) = LocalDateTime.parse(s).toEpochSecond(ZoneOffset.UTC)
    HourPartitioner.partition(0) should be("1970-01-01-00")
    HourPartitioner.partition(e("2000-01-01T00:00:00")) should be("2000-01-01-00")
    HourPartitioner.partition(e("2000-01-01T23:59:59")) should be("2000-01-01-23")
  }

  "The identity partitioner" should "partition at day level" in {
    IdentityPartitioner.partition(0) should be("0")
    IdentityPartitioner.partition(1) should be("1")
  }

}
