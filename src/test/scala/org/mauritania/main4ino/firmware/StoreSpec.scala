package org.mauritania.main4ino.firmware

import java.nio.file.Paths

import cats.effect.IO
import org.mauritania.main4ino.TmpDirCtx
import org.scalatest._
import org.scalatest.EitherValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StoreSpec extends AnyFlatSpec with Matchers with TmpDirCtx with ParallelTestExecution {

  "The store" should "point to an existent file given the coordinates" in {
    val path = Paths.get("src", "test", "resources", "firmwares", "1")
    val store = new Store[IO](path)
    store.getFirmware(Wish("botino", "1.0.0", "esp8266")).unsafeRunSync().right.value.filename should be("firmware-1.0.0.esp8266.bin")
  }

  it should "report a meaningful failure when cannot find a file" in {
    val store = new Store[IO](Paths.get("non", "existent", "path"))
    store.getFirmware(Wish("botino", "2.0.0", "esp8266")).unsafeRunSync().left.value should (include("2.0.0") and include("botino"))
  }

  it should "list firmware coordinates" in {
    val file = Paths.get("src", "test", "resources", "firmwares", "1")
    val store = new Store[IO](file)
    store.listFirmwares("botino", "esp8266").unsafeRunSync() should
      be(Seq(Coord("botino", "1.0.0", "esp8266", "firmware-1.0.0.esp8266.bin")))
  }

  it should "resolve latest version" in {
    val file = Paths.get("src", "test", "resources", "firmwares", "2")
    val store = new Store[IO](file)
    store.getFirmware(Wish("botino", "LATEST", "esp8266")).unsafeRunSync().right.value.filename should be("firmware-2.0.0.esp8266.bin")
  }

  it should "report a meaningful failure when cannot download a file" in {
    val file = Paths.get("src", "test", "resources", "firmwares", "3")
    val store = new Store[IO](file)
    val x = for {
      a <- store.getFirmware(Wish("botino", "1.1.0", "esp8266"))
      b <- store.getFirmwareFile(a.toOption.get)
    } yield b
    x.unsafeRunSync().left.value should (include("Could not locate/read"))
  }

  it should "report a meaningful failure when cannot find a version among available ones" in {
    val file = Paths.get("src", "test", "resources", "firmwares", "3")
    val store = new Store[IO](file)
    store.getFirmware(Wish("botino", "1.1.1", "esp8266")).unsafeRunSync().left.value should (include("Could not resolve"))
  }

  it should "list firmware coordinates (malformed files)" in {
    val file = Paths.get("src", "test", "resources", "firmwares", "4")
    val store = new Store[IO](file)
    store.listFirmwares("botino", "esp8266").unsafeRunSync() should be(Seq())
  }

  it should "resolve latest stable version (only release versions)" in {
    val file = Paths.get("src", "test", "resources", "firmwares", "5")
    val store = new Store[IO](file)
    store.getFirmware(Wish("botino", "LATEST", "esp8266")).unsafeRunSync().right.value.filename should be("firmware-1.0.1-1.esp8266.bin")
    store.getFirmware(Wish("botino", "LATEST_STABLE", "esp8266")).unsafeRunSync().right.value.filename should be("firmware-1.0.0.esp8266.bin")
  }

  it should "resolve latest version with feature" in {
    val file = Paths.get("src", "test", "resources", "firmwares", "6")
    val store = new Store[IO](file)
    store.getFirmware(Wish("botino", "LATEST", "esp8266")).unsafeRunSync().right.value.filename should be("firmware-1.0.0.esp8266.bin")
    store.getFirmware(Wish("botino", "LATEST_STABLE", "esp8266")).unsafeRunSync().right.value.filename should be("firmware-1.0.0.esp8266.bin")
    store.getFirmware(Wish("botino", "LATEST_performance", "esp8266")).unsafeRunSync().right.value.filename should be("firmware-1.0.0_performance.esp8266.bin")
    store.getFirmware(Wish("botino", "1.0.0_performance", "esp8266")).unsafeRunSync().right.value.filename should be("firmware-1.0.0_performance.esp8266.bin")
  }

  it should "have good orderings among multiple majors / minors" in {
    val v100 = List(
      "1.0.0-d5199da",
    )
    val v012 = List(
      "0.12.12-d5e99da",
      "0.12.2-865204d",
      "0.12.13-5d1aaf3",
      "0.12.1-35c9114",
      "0.12.9-969ddf5",
      "0.12.9-SNAPSHOT-7b13405",
      "0.12.1-SNAPSHOT-96a51a3",
      "0.12.0-576ae43",
      "0.12.7-d0ae9ef"
    )
    val v011 = List(
      "0.11.2-0020bc2",
      "0.11.0-c0b0dc4",
      "0.11.1-4cd8a3d"
    )

    val v012FC = v012.map(v => Coord("", v, "", s"firmware-$v..bin"))
    v012FC.max(Store.ByCoordsVer).version should be("0.12.13-5d1aaf3")
    v012FC.min(Store.ByCoordsVer).version should be("0.12.0-576ae43")

    val v011FC = v011.map(v => Coord("", v, "", s"firmware-$v..bin"))
    v011FC.max(Store.ByCoordsVer).version should be("0.11.2-0020bc2")
    v011FC.min(Store.ByCoordsVer).version should be("0.11.0-c0b0dc4")

    val vmixFC = (v100 ++ v012).map(v => Coord("", v, "", s"firmware-$v..bin"))
    vmixFC.max(Store.ByCoordsVer).version should be("1.0.0-d5199da")
    vmixFC.min(Store.ByCoordsVer).version should be("0.12.0-576ae43")

  }

  it should "have good orderings with same majors / minors" in {
    val v10 = List(
      "1.0.0-555",
      "1.0.1",
      "1.0.1-444"
    )
    val v10FC = v10.map(v => Coord("", v, "", s"firmware-$v..bin"))
    v10FC.max(Store.ByCoordsVer).version should be("1.0.1")
    v10FC.min(Store.ByCoordsVer).version should be("1.0.0-555")
  }


}
