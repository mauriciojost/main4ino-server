package org.mauritania.main4ino.firmware

import java.nio.file.{Path, Paths}

import cats.effect.IO
import org.http4s.{Method, Request, Response, Status, Uri}
import org.mauritania.main4ino.TmpDir
import org.mauritania.main4ino.firmware.Store.FirmwareCoords
import org.scalatest._

import io.circe.syntax._
import org.http4s.circe._
import io.circe.generic.auto._

class ServiceFuncSpec extends FlatSpec with Matchers with TmpDir {

  final val Byte0: Byte = '0'.toByte
  final val Byte1: Byte = '1'.toByte
  final val ByteEnd: Byte = 10.toByte

  final val Dataset1 = Paths.get("src", "test", "resources", "firmwares", "1")

  def defaultServiceWithDirectory(tmp: Path): Service[IO] = new Service(new StoreIO(tmp))

  "The service" should "download a given firmware" in {

    implicit val s = defaultServiceWithDirectory(Dataset1)

    val rs = get("/firmwares/botino/esp8266/content?version=1.0.0") // available
    rs.status shouldBe Status.Ok
    rs.contentLength shouldBe Some(5L)
    rs.body.compile.toList.unsafeRunSync() shouldBe List(Byte0, Byte0, Byte1, Byte1, ByteEnd)

    val rf = get("/firmwares/botino/esp32/content?version=1.0.0") // not available
    rf.status shouldBe Status.NoContent
    rf.body.compile.toList.unsafeRunSync() shouldBe List()

  }

  it should "reply with not found when mandatory parameters are not provided" in {
    implicit val s = defaultServiceWithDirectory(Paths.get("xxx"))
    get("/firmwares/botino/esp8266").status shouldBe Status.NotFound
  }

  it should "list firmware coordinates" in {
    implicit val s = defaultServiceWithDirectory(Dataset1)
    val r = get("/firmwares/botino")
    r.status shouldBe Status.Ok
    r.bodyAsText.compile.toList.unsafeRunSync().head shouldBe Set(FirmwareCoords("botino", "1.0.0", "esp8266")).asJson.noSpaces
  }

  private[this] def get(path: String)(implicit service: Service[IO]): Response[IO] = {
    val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(path))
    service.request(request).unsafeRunSync()
  }

}
