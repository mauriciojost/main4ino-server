package org.mauritania.main4ino.helpers

import java.nio.file.Paths
import java.time.{Instant, ZoneId, ZonedDateTime}

import cats.effect.IO
import fs2.Stream
import org.mauritania.main4ino.TmpDir
import org.scalatest._

import scala.io.Source

class DevLoggerSpec extends FlatSpec with Matchers with TmpDir {

  val TheTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))

  class FixedTimeIO extends Time[IO] {
    def nowUtc: IO[ZonedDateTime] = IO.pure(TheTime)
  }

  "The logger" should "append a message to a file" in {
    withTmpDir { tmp =>
      val expectedFile = tmp.resolve("device.log")
      val logger = new DevLoggerIO(tmp, new FixedTimeIO())
      val s1 = Stream("hey\n", "you\n") // creates and appends
      logger.updateLogs("device", s1).unsafeRunSync() should be(Right(()))
      Source.fromFile(expectedFile.toFile).getLines.toList should be(
        List(
          "1970-01-01T00:00Z[UTC] hey",
          "1970-01-01T00:00Z[UTC] you"
        )
      )
      val s2 = Stream("guy\n") // appends
      logger.updateLogs("device", s2).unsafeRunSync() should be(Right(()))
      Source.fromFile(expectedFile.toFile).getLines.toList should be(
        List(
          "1970-01-01T00:00Z[UTC] hey",
          "1970-01-01T00:00Z[UTC] you",
          "1970-01-01T00:00Z[UTC] guy" // << extra line appended
        )
      ) // appends
    }
  }

  it should "report a meaningful failure when cannot write file" in {
    val logger = new DevLoggerIO(Paths.get("/non/existent/path"), new FixedTimeIO())
    val s = Stream("hey")
    logger.updateLogs("device", s).unsafeRunSync().left.get should include("/non/existent/path")
  }

}
