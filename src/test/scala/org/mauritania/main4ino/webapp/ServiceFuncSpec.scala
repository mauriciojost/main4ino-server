package org.mauritania.main4ino.webapp

import cats.effect.IO
import org.http4s.{Method, Request, Response, Status, Uri}
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers, Sequential}

class ServiceFuncSpec extends FlatSpec with Matchers with BeforeAndAfterEach {

  Sequential

  "The service" should "read existent index.html" in {
    val indexHtml = getExpectOk("/index.html")(new Service("/webapp/index.html"))
    indexHtml should include("</body>")
  }

  it should "read existent index.html (from root)" in {
    val root = getExpectOk("/")(new Service("/webapp/index.html"))
    root should include("</body>") // index.html
  }

  it should "fail to read non-existent index.html (from root)" in {
    get("/")(new Service("/somewhere/index.html")).status shouldBe Status.InternalServerError
  }

  it should "read existent app.js" in {
    val appJs = getExpectOk("/js/app.js")(new Service("/webapp/index.html"))
    appJs should include("=") // app.js
  }

  private[this] def getExpectOk(path: String)(implicit service: Service): String = {
    val r = get(path)
    r.status shouldBe Status.Ok
    r.bodyAsText.compile.toList.unsafeRunSync().mkString("\n")
  }

  private[this] def get(path: String)(implicit service: Service): Response[IO] = {
    val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(path))
    service.request(request).unsafeRunSync()
  }

}
