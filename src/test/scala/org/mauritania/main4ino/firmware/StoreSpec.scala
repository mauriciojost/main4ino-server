package org.mauritania.main4ino.firmware

import java.nio.file.Paths

import cats.effect.IO
import org.mauritania.main4ino.TmpDir
import org.mauritania.main4ino.firmware.Store.FirmwareCoords
import org.scalatest._
import org.scalatest.EitherValues._

class StoreSpec extends FlatSpec with Matchers with TmpDir {

  "The store" should "point to an existent file given the coordinates" in {
    val path = Paths.get("src", "test", "resources", "firmwares", "1")
    val store = new StoreIO[IO](path)
    store.getFirmware(FirmwareCoords("botino", "1.0.0", "esp8266")).unsafeRunSync().right.value.file.getName should be("firmware-1.0.0.esp8266.bin")
  }

  it should "report a meaningful failure when cannot find a file" in {
    val store = new StoreIO[IO](Paths.get("non", "existent", "path"))
    store.getFirmware(FirmwareCoords("botino", "2.0.0", "esp8266")).unsafeRunSync().left.value should (include("2.0.0") and include("botino"))
  }

  it should "list firmware coordinates" in {
    val file = Paths.get("src", "test", "resources", "firmwares", "1")
    val store = new StoreIO[IO](file)
    store.listFirmwares("botino", "esp8266").unsafeRunSync() should be(Set(FirmwareCoords("botino", "1.0.0", "esp8266")))
  }

  it should "resolve latest version" in {
    val file = Paths.get("src", "test", "resources", "firmwares", "2")
    val store = new StoreIO[IO](file)
    store.getFirmware(FirmwareCoords("botino", "LATEST", "esp8266")).unsafeRunSync().right.value.file.getName should be("firmware-2.0.0.esp8266.bin")
  }

  it should "report a meaningful failure when cannot download a file" in {
    val file = Paths.get("src", "test", "resources", "firmwares", "3")
    val store = new StoreIO[IO](file)
    store.getFirmware(FirmwareCoords("botino", "1.1.0", "esp8266")).unsafeRunSync().left.value should (include("Could not locate/read"))
  }


}
