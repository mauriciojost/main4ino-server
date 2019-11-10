package org.mauritania.main4ino.firmware

import java.nio.file.Paths

import org.mauritania.main4ino.TmpDir
import org.scalatest._
import org.scalatest.EitherValues._

class StoreSpec extends FlatSpec with Matchers with TmpDir {

  final val Byte0: Byte = '0'.toByte
  final val Byte1: Byte = '1'.toByte
  final val ByteEnd: Byte = 10.toByte

  "The store" should "read an existent file" in {
    val file = Paths.get("src", "test", "resources", "firmwares", "1")
    val store = new StoreIO(file)
    store.getFirmware("botino", "firmware")
      .unsafeRunSync().right.value
      .compile.toList.unsafeRunSync() should be(List(Byte0, Byte0, Byte1, Byte1, ByteEnd))
  }

  it should "report a meaningful failure when cannot find a file" in {
    val store = new StoreIO(Paths.get("/non/existent/path"))
    store.getFirmware("botino", "firmware").unsafeRunSync().left.value should (include("firmware") and include("botino"))
  }

}
