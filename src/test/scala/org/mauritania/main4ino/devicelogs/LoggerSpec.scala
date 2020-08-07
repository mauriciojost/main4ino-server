package org.mauritania.main4ino.devicelogs

import java.nio.file.{NoSuchFileException, Path, Paths}
import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneId, ZoneOffset, ZonedDateTime}

import cats.effect.{IO, Sync}
import eu.timepit.refined.types.numeric.{NonNegInt, PosInt}
import fs2.Stream
import org.mauritania.main4ino.TmpDirCtx
import org.mauritania.main4ino.devicelogs.Partitioner.{EpochSecPartitioner, HourPartitioner, Partitioner}
import org.mauritania.main4ino.helpers.Time
import org.mauritania.main4ino.models.EpochSecTimestamp
import org.scalatest.EitherValues._
import org.scalatest.ParallelTestExecution
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext
import scala.io.Source

class LoggerSpec extends AnyFlatSpec with Matchers with TmpDirCtx with ParallelTestExecution {

  class FixedTime(t: EpochSecTimestamp) extends Time[IO] {
    override def nowUtc: IO[ZonedDateTime] = IO.pure(ZonedDateTime.ofInstant(Instant.ofEpochSecond(t), ZoneId.of("UTC")))
  }

  "The logger" should "append a message to a file and read it" in {
    withTmpDir { tmp =>
      val expectedFile = tmp.resolve("device.0.log")
      val logger0 = buildLogger(tmp, 0) // time 0
      logger0.updateLogs("device", Stream("hey\nyou\n")/* creates and appends */).unsafeRunSync().right.value should be(12L)
      Source.fromFile(expectedFile.toFile).getLines.toList should be(List("0 hey", "0 you"))
      logger0.updateLogs("device", Stream("guy\n")/* appends */).unsafeRunSync().right.value should be(6L)
      Source.fromFile(expectedFile.toFile).getLines.toList should be(List("0 hey", "0 you", "0 guy")) // appends
      val logger1 = buildLogger(tmp, 1) // time 1
      logger1.updateLogs("device", Stream("how\n")/* appends */).unsafeRunSync().right.value should be(6L)

      val logger2 = buildLogger(tmp, 2) // time 2
      val readFull = logger2.getLogs("device", 0L, 2L).unsafeRunSync()
      readFull.compile.toList.unsafeRunSync() should be(List("0 hey", "0 you", "0 guy", "1 how"))

      val readIngoreLength1 =
        logger2.getLogs("device", 1L, 3L).unsafeRunSync()
      readIngoreLength1.compile.toList.unsafeRunSync() should be(List("1 how"))

      val readIngoreLength2 =
        logger2.getLogs("device", 0L, 0L).unsafeRunSync()
      readIngoreLength2.compile.toList.unsafeRunSync() should be(List("0 hey", "0 you", "0 guy"))
    }
  }

  it should "limit retrieval length" in {
    withTmpDir { tmp =>
      val loggerWrite = buildLogger(tmp = tmp, t = 0)
      val s = Stream("hey\nyou")
      loggerWrite.updateLogs("device", s).unsafeRunSync().right.value should be(12L)

      val loggerRead12 = buildLogger(tmp = tmp, t = 0, mxLen = PosInt((1/*time*/ + 1/*space*/ + 3/*hey*/ + 1/*newline*/)* 2))
      val read12 = loggerRead12.getLogs("device", 0L, 0L).unsafeRunSync()
      read12.compile.toList.unsafeRunSync() should be(List("0 hey", "0 you"))

      val loggerRead6 = buildLogger(tmp = tmp, t = 0, mxLen = PosInt(6))
      val read6 = loggerRead6.getLogs("device", 0L, 0L).unsafeRunSync()
      read6.compile.toList.unsafeRunSync() should be(List("0 you"))
    }
  }


  it should "report a meaningful failure when cannot write file" in {
    val logger = buildLogger(Paths.get("/non/existent/path"))
    val s = Stream("hey")
    logger.updateLogs("device", s).unsafeRunSync().left.value should include("/non/existent/path")
  }

  it should "report a meaningful failure when cannot read file" in {
    val logger = buildLogger(Paths.get("/non/existent/path"))
    logger.getLogs("device1", 0L, 10L).unsafeRunSync().compile.toList.unsafeRunSync() should be(List.empty[String])
  }

  it should "use correctly the daily partitioner" in {
    withTmpDir { tmp =>
      val dt = LocalDateTime.parse("2020-01-01T00:00:00").atOffset(ZoneOffset.UTC)
      val es = dt.toEpochSecond
      val loggerWrite = buildLogger(tmp = tmp, t = es, HourPartitioner)
      val s = Stream("hey\nyou")
      loggerWrite.updateLogs("device", s).unsafeRunSync().right.value should be(30L)
      val expectedFile = tmp.resolve("device.2020-01-01-00.log")
      Source.fromFile(expectedFile.toFile).getLines.toList should be(List(es + " hey", es + " you"))
    }
  }

  it should "filter log messages while reading using partition and record" in {
    val dev = "device"
    withTmpDir { tmp =>
      def dt(h: Int, m: Int = 0): Long = LocalDateTime.parse(s"2020-01-01T0$h:0$m:00").atOffset(ZoneOffset.UTC).toEpochSecond
      def loggerAt(h: Int, m: Int = 0): Logger[IO] = buildLogger(tmp = tmp, t = dt(h, m), HourPartitioner, bypass = 2)

      // Insert some logs (2 in same partition, 2 in other different partitions)
      loggerAt(1, 0).updateLogs(dev, Stream("a")).unsafeRunSync().right.value should be(13L) // partition 1 (hourly)
      loggerAt(1, 1).updateLogs(dev, Stream("b")).unsafeRunSync().right.value should be(13L) // partition 1
      loggerAt(2).updateLogs(dev, Stream("c")).unsafeRunSync().right.value should be(13L) // partition 2
      loggerAt(3).updateLogs(dev, Stream("d")).unsafeRunSync().right.value should be(13L) // partition 3

      loggerAt(4).getLogs(dev, dt(1, 1), dt(3)).unsafeRunSync().compile.toList.unsafeRunSync() should be(
        List(
          s"${dt(1, 0)} a",
          s"${dt(1, 1)} b",
          s"${dt(2)} c",
          s"${dt(3)} d"
        )
      ) // 'a' not being dropped because 3 partitions involved, so no record filtering performed
      loggerAt(4).getLogs(dev, dt(1, 1), dt(2)).unsafeRunSync().compile.toList.unsafeRunSync() should be(
        List(
          s"${dt(1, 1)} b",
          s"${dt(2)} c"
        )
      ) // 'a' being dropped because 2 partitions involved, so record filtering performed
    }
  }


  private def buildLogger(
    tmp: Path,
    t: EpochSecTimestamp = 0L,
    part: Partitioner = EpochSecPartitioner,
    mxLen: PosInt = PosInt(1024),
    bypass: Int = 1
  ): Logger[IO] = {
    val ec = ExecutionContext.global
    val logger = new Logger[IO](Config(tmp, mxLen, part, bypass), new FixedTime(t), ec)(Sync[IO], IO.contextShift(ec))
    logger
  }

}
