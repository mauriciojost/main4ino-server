package org.mauritania.main4ino.firmware

import java.nio.file.Paths

import org.mauritania.main4ino.TmpDir
import org.mauritania.main4ino.firmware.Store.FirmwareCoords
import org.scalatest._
import org.scalatest.EitherValues._

class StoreSpec extends FlatSpec with Matchers with TmpDir {

  "The store" should "point to an existent file given the coordinates" in {
    val path = Paths.get("src", "test", "resources", "firmwares", "1")
    val store = new StoreIO(path)
    store.getFirmware(FirmwareCoords("botino", "1.0.0", "esp8266")).unsafeRunSync().right.value.file.getName should be("firmware-1.0.0.esp8266.bin")
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
