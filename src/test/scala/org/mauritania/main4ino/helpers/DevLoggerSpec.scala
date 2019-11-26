package org.mauritania.main4ino.helpers

import java.nio.file.{Path, Paths}
import java.time.{Instant, ZoneId, ZonedDateTime}

import cats.effect.{IO, Sync}
import fs2.Stream
import org.mauritania.main4ino.TmpDirCtx
import org.scalatest._
import org.scalatest.EitherValues._

import scala.concurrent.ExecutionContext
import scala.io.Source

class DevLoggerSpec extends FlatSpec with Matchers with TmpDirCtx {

  val TheTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))

  class FixedTime extends Time[IO] {
    override def nowUtc: IO[ZonedDateTime] = IO.pure(TheTime)
  }

  "The logger" should "append a message to a file and read it" in {
    withTmpDir { tmp =>
      val expectedFile = tmp.resolve("device.log")
      val logger= buildLogger(tmp)
      val s1 = Stream("hey\nyou\n") // creates and appends
      logger.updateLogs("device", s1).unsafeRunSync() should be(Right(()))
      Source.fromFile(expectedFile.toFile).getLines.toList should be(
        List(
          "### 1970-01-01T00:00Z[UTC]",
          "hey",
          "you"
        )
      )
      val s2 = Stream("guy\n") // appends
      logger.updateLogs("device", s2).unsafeRunSync() should be(Right(()))
      Source.fromFile(expectedFile.toFile).getLines.toList should be(
        List(
          "### 1970-01-01T00:00Z[UTC]",
          "hey",
          "you",
          "### 1970-01-01T00:00Z[UTC]",  // << extra appended section from here
          "guy"
        )
      ) // appends

      val readFull = logger.getLogs("device", None, None).unsafeRunSync()
      val successfulReadFull = readFull.right.value
      successfulReadFull.compile.toList.unsafeRunSync().mkString should be(
        """### 1970-01-01T00:00Z[UTC]
          |hey
          |you
          |### 1970-01-01T00:00Z[UTC]
          |guy
          |""".stripMargin
      )

      val readIngoreLength1 =
        logger.getLogs("device", Some(1L)/*new line*/, Some(3L)).unsafeRunSync()
      val successfulReadIgnoreLength1 = readIngoreLength1.right.value
      successfulReadIgnoreLength1.compile.toList.unsafeRunSync().mkString should be("guy")

      val readIngoreLength2 =
        logger.getLogs("device", Some(2L /*new lines*/ + 3L /*guy*/), Some(5L)).unsafeRunSync()
      val successfulReadIgnoreLength2 = readIngoreLength2.right.value
      successfulReadIgnoreLength2.compile.toList.unsafeRunSync().mkString should be("[UTC]")
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

  private def buildLogger(tmp: Path) = {
    val ec = ExecutionContext.global
    val logger = new DevLogger[IO](tmp, new FixedTime(), ec)(Sync[IO], IO.contextShift(ec))
    logger
  }

}
