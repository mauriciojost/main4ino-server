package org.mauritania.main4ino.firmware

import java.nio.file.Paths

import org.mauritania.main4ino.TmpDir
import org.mauritania.main4ino.firmware.Store.FirmwareCoords
import org.scalatest._
import org.scalatest.EitherValues._

class StoreSpec extends FlatSpec with Matchers with TmpDir {

  final val Byte0: Byte = '0'.toByte
  final val Byte1: Byte = '1'.toByte
  final val ByteEnd: Byte = 10.toByte

  "The store" should "read an existent file" in {
    val file = Paths.get("src", "test", "resources", "firmwares", "1")
    val store = new StoreIO(file)
    store.getFirmware(FirmwareCoords("botino", "1.0.0", "esp8266"))
      .unsafeRunSync().right.value
      .compile.toList.unsafeRunSync() should be(List(Byte0, Byte0, Byte1, Byte1, ByteEnd))
  }

  it should "report a meaningful failure when cannot find a file" in {
    val store = new StoreIO(Paths.get("/non/existent/path"))
    store.getFirmware(FirmwareCoords("botino", "2.0.0", "esp8266")).unsafeRunSync().left.value should (include("firmware") and include("botino"))
  }

  it should "list firmware coordinates" in {
    val file = Paths.get("src", "test", "resources", "firmwares", "1")
    val store = new StoreIO(file)
    store.listFirmwares("botino")
      .unsafeRunSync() should be(Set(FirmwareCoords("botino", "1.0.0", "esp8266")))
  }

}
