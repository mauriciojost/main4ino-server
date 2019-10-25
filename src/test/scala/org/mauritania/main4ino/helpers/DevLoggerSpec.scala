package org.mauritania.main4ino.helpers

import java.nio.file.Paths
import fs2.Stream

import org.scalatest._
import org.scalatest.EitherValues._

class DevLoggerSpec extends FlatSpec with Matchers {

  "The logger" should "append a message to a file" in {
    val logger = new DevLoggerIO(Paths.get("/tmp"))
    val s = Stream("hey", "you", "guy")
    logger.updateLogs("device", s).unsafeRunSync() should be (Right(()))
  }

  it should "report a meaningful failure when cannot write file" in {
    val logger = new DevLoggerIO(Paths.get("/non/existent/path"))
    val s = Stream("hey", "you", "guy")
    logger.updateLogs("device", s).unsafeRunSync().left.get should include ("/non/existent/path")
  }

}
