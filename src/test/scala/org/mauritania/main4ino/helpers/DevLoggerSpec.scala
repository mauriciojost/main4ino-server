package org.mauritania.main4ino.helpers

import java.nio.file.Paths

import fs2.Stream
import org.mauritania.main4ino.TmpDir
import org.scalatest._
import org.scalatest.EitherValues._

import scala.io.Source

class DevLoggerSpec extends FlatSpec with Matchers with TmpDir {

  "The logger" should "append a message to a file" in {
    withTmpDir { tmp =>
      val expectedFile = tmp.resolve("device.log")
      val logger = new DevLoggerIO(tmp)
      val s1 = Stream("hey\n", "you\n") // creates and appends
      logger.updateLogs("device", s1).unsafeRunSync() should be(Right(()))
      Source.fromFile(expectedFile.toFile).getLines.toList should be(List("hey", "you"))
      val s2 = Stream("guy\n") // appends
      logger.updateLogs("device", s2).unsafeRunSync() should be(Right(()))
      Source.fromFile(expectedFile.toFile).getLines.toList should be(List("hey", "you", "guy")) // appends
    }
  }

  it should "report a meaningful failure when cannot write file" in {
    val logger = new DevLoggerIO(Paths.get("/non/existent/path"))
    val s = Stream("hey")
    logger.updateLogs("device", s).unsafeRunSync().left.get should include ("/non/existent/path")
  }

}
