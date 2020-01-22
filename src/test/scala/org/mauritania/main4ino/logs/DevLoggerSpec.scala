package org.mauritania.main4ino.logs

import java.nio.file.{Path, Paths}
import java.time.{Instant, ZoneId, ZonedDateTime}

import cats.effect.{IO, Sync}
import fs2.Stream
import org.mauritania.main4ino.Config.DevLoggerConfig
import org.mauritania.main4ino.TmpDirCtx
import org.mauritania.main4ino.helpers.Time
import org.mauritania.main4ino.models.EpochSecTimestamp
import org.scalatest.EitherValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext
import scala.io.Source

class DevLoggerSpec extends AnyFlatSpec with Matchers with TmpDirCtx {

  class FixedTime(t: EpochSecTimestamp) extends Time[IO] {
    override def nowUtc: IO[ZonedDateTime] = IO.pure(ZonedDateTime.ofInstant(Instant.ofEpochSecond(t), ZoneId.of("UTC")))
  }

  "The logger" should "append a message to a file and read it" in {
    withTmpDir { tmp =>
      val expectedFile = tmp.resolve("device.log")
      val logger0= buildLogger(tmp, 0)
      val s1 = Stream("hey\nyou\n") // creates and appends
      logger0.updateLogs("device", s1).unsafeRunSync() should be(Right(()))
      Source.fromFile(expectedFile.toFile).getLines.toList should be(
        List(
          "0 hey",
          "0 you"
      )
      )
      val s2 = Stream("guy\n") // appends
      val logger1= buildLogger(tmp, 1)
      logger1.updateLogs("device", s2).unsafeRunSync() should be(Right(()))
      Source.fromFile(expectedFile.toFile).getLines.toList should be(
        List(
          "0 hey",
          "0 you",
          "1 guy"
      )
      ) // appends

      val logger2= buildLogger(tmp, 2)
      val readFull = logger2.getLogs("device", None, None).unsafeRunSync()
      val successfulReadFull = readFull.right.value
      successfulReadFull.compile.toList.unsafeRunSync() should be(
        List(
          LogRecord(0, "hey"),
          LogRecord(0, "you"),
          LogRecord(1, "guy")
        )
      )

      val readIngoreLength1 =
        logger2.getLogs("device", Some(1L), Some(3L)).unsafeRunSync()
      val successfulReadIgnoreLength1 = readIngoreLength1.right.value
      successfulReadIgnoreLength1.compile.toList.unsafeRunSync() should be(List(LogRecord(1L, "guy")))

      val readIngoreLength2 =
        logger2.getLogs("device", Some(0L), Some(0L)).unsafeRunSync()
      val successfulReadIgnoreLength2 = readIngoreLength2.right.value
      successfulReadIgnoreLength2.compile.toList.unsafeRunSync() should be(List(LogRecord(0L, "hey"), LogRecord(0L, "you")))
    }
  }

  it should "report a meaningful failure when cannot write file" in {
    val logger = buildLogger(Paths.get("/non/existent/path"))
    val s = Stream("hey")
    logger.updateLogs("device", s).unsafeRunSync().left.get should include("/non/existent/path")
  }

  it should "report a meaningful failure when cannot read file" in {
    val logger = buildLogger(Paths.get("/non/existent/path"))
    logger.getLogs("device1", None, None).unsafeRunSync().left.get should include("device1")
  }

  private def buildLogger(tmp: Path, t: EpochSecTimestamp = 0L) = {
    val ec = ExecutionContext.global
    val logger = new DevLogger[IO](DevLoggerConfig(tmp), new FixedTime(t), ec)(Sync[IO], IO.contextShift(ec))
    logger
  }

}
