package org.mauritania.main4ino.firmware

import java.nio.file.{Path, Paths}

import cats.effect.{Effect, IO, Sync}
import org.http4s.{Header, Headers, Method, Request, Response, Status, Uri}
import org.mauritania.main4ino.TmpDirCtx
import org.mauritania.main4ino.firmware.Store.FirmwareCoords
import org.scalatest._
import io.circe.syntax._
import org.http4s.circe._
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext

class ServiceFuncSpec extends FlatSpec with Matchers with TmpDirCtx {

  final val Byte0: Byte = '0'.toByte
  final val Byte1: Byte = '1'.toByte
  final val ByteEnd: Byte = 10.toByte

  final val Dataset1 = Paths.get("src", "test", "resources", "firmwares", "1")


  def defaultServiceWithDirectory(tmp: Path): Service[IO] =
    new Service[IO](new Store[IO](tmp), ExecutionContext.global)(Sync[IO], Effect[IO], IO.contextShift(ExecutionContext.global))

  "The service" should "download a given firmware" in {

    implicit val s = defaultServiceWithDirectory(Dataset1)

    val rs = get("/botino/esp8266/content?version=1.0.0") // available
    rs.status shouldBe Status.Ok
    rs.contentLength shouldBe Some(5L)
    rs.body.compile.toList.unsafeRunSync() shouldBe List(Byte0, Byte0, Byte1, Byte1, ByteEnd)

    val rf = get("/botino/esp32/content?version=1.0.0") // not available
    rf.status shouldBe Status.NotFound
    rf.body.compile.toList.unsafeRunSync() shouldBe List()

  }

  it should "reply with not found when mandatory parameters are not provided" in {
    implicit val s = defaultServiceWithDirectory(Paths.get("xxx"))
    get("/botino/esp8266/content")(s).status shouldBe Status.NotFound
  }

  it should "list firmware coordinates" in {
    implicit val s = defaultServiceWithDirectory(Dataset1)
    val r = get("/botino/esp8266")
    r.status shouldBe Status.Ok
    r.bodyAsText.compile.toList.unsafeRunSync().head shouldBe Set(FirmwareCoords("botino", "1.0.0", "esp8266")).asJson.noSpaces
  }

  it should "tell that current version is up to date" in {
    implicit val s = defaultServiceWithDirectory(Dataset1)
    val rs = get(
      path = "/botino/esp8266/content?version=1.0.0",
      headers = Headers(Header(Service.Esp8266VersionHeader, "1.0.0"))
    )
    rs.status shouldBe Status.NotModified // already up to date
  }

  it should "handle scenario of firmware providing no version" in {
    implicit val s = defaultServiceWithDirectory(Dataset1)
    val rs = get(
      path = "/botino/esp8266/content?version=1.0.0"
      //headers = Headers(Header(Service.Esp8266VersionHeader, "1.0.0"))
    )
    rs.status shouldBe Status.Ok // can be downloaded
  }

  it should "handle scenario of firmware providing too many versions" in {
    implicit val s = defaultServiceWithDirectory(Dataset1)
    val rs = get(
      path = "/botino/esp8266/content?version=1.0.0",
      headers = Headers(Header(Service.Esp8266VersionHeader, "1.0.0"), Header(Service.Esp32VersionHeader, "2.0.0"))
    )
    rs.status shouldBe Status.Ok // can be downloaded
  }

  private[this] def get(path: String, headers: Headers = Headers.empty)(implicit service: Service[IO]): Response[IO] = {
    val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(path), headers = headers)
    service.request(request).unsafeRunSync()
  }

}
