package org.mauritania.main4ino.webapp

import cats.effect.IO
import org.http4s.{Method, Request, Response, Status, Uri}
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers, Sequential}

class ServiceFuncSpec extends FlatSpec with Matchers with BeforeAndAfterEach {

  // TODO this test is high level (hits REST API via real http)
  // what kind of tests should go here?
  // what kind of tests should go at a deeper level (without REST)?
  // when to stop caring about test coverage?
  Sequential

  "The service" should "read existent index.html" in {
    val indexHtml = getExpectOk("/index.html")(new Service())
    indexHtml should include("</body>")
  }

  it should "read existent index.html (from root)" in {
    val root = getExpectOk("/")(new Service())
    root should include("</body>") // index.html
  }

  it should "read existent app.js" in {
    val appJs = getExpectOk("/js/app.js")(new Service())
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
