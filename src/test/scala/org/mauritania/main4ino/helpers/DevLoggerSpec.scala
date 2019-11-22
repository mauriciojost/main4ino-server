package org.mauritania.main4ino.helpers

import java.nio.file.Paths
import java.time.{Instant, ZoneId, ZonedDateTime}

import cats.effect.IO
import fs2.Stream
import org.mauritania.main4ino.TmpDir
import org.scalatest._
import org.scalatest.EitherValues._

import scala.io.Source

class DevLoggerSpec extends FlatSpec with Matchers with TmpDir {

  val TheTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))

  class FixedTimeIO extends Time[IO] {
    def nowUtc: IO[ZonedDateTime] = IO.pure(TheTime)
  }

  "The logger" should "append a message to a file and read it" in {
    withTmpDir { tmp =>
      val expectedFile = tmp.resolve("device.log")
      val logger = new DevLoggerIO(tmp, new FixedTimeIO())
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

      val readFull = logger.getLogs("device", 0L, 1024L).unsafeRunSync()
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
        logger.getLogs("device", 1L/*new line*/, 3L).unsafeRunSync()
      val successfulReadIgnoreLength1 = readIngoreLength1.right.value
      successfulReadIgnoreLength1.compile.toList.unsafeRunSync().mkString should be("guy")

      val readIngoreLength2 =
        logger.getLogs("device", 2L /*new lines*/ + 3L /*guy*/, 5L).unsafeRunSync()
      val successfulReadIgnoreLength2 = readIngoreLength2.right.value
      successfulReadIgnoreLength2.compile.toList.unsafeRunSync().mkString should be("[UTC]")
    }
  }

  it should "report a meaningful failure when cannot write file" in {
    val logger = new DevLoggerIO(Paths.get("/non/existent/path"), new FixedTimeIO())
    val s = Stream("hey")
    logger.updateLogs("device", s).unsafeRunSync().left.get should include("/non/existent/path")
  }

  it should "report a meaningful failure when cannot read file" in {
    val logger = new DevLoggerIO(Paths.get("/non/existent/path"), new FixedTimeIO())
    logger.getLogs("device1", 0L, 1024L).unsafeRunSync().left.get should include("device1")
  }

}
