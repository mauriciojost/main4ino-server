package org.mauritania.main4ino.webapp

import cats.effect.{Blocker, Effect, IO, Sync}
import org.http4s.{Method, Request, Response, Status, Uri}
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers, Sequential}

import scala.concurrent.ExecutionContext

class ServiceFuncSpec extends FlatSpec with Matchers with BeforeAndAfterEach {

  Sequential

  "The service" should "read existent index.html" in {
    val indexHtml = getExpectOk("/index.html")(buildService())
    indexHtml should include("</body>")
  }

  it should "read existent index.html (from root)" in {
    val root = getExpectOk("/")(buildService())
    root should include("</body>") // index.html
  }

  it should "fail to read non-existent index.html (from root)" in {
    get("/")(buildService("/somewhere/index.html")).status shouldBe Status.InternalServerError
  }

  it should "read existent app.js" in {
    val appJs = getExpectOk("/js/app.js")(buildService())
    appJs should include("=") // app.js
  }

  private[this] def getExpectOk(path: String)(service: Service[IO]): String = {
    val r = get(path)(service)
    r.status shouldBe Status.Ok
    r.bodyAsText.compile.toList.unsafeRunSync().mkString("\n")
  }

  private[this] def get(path: String)(service: Service[IO]): Response[IO] = {
    val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(path))
    service.request(request).unsafeRunSync()
  }

  private def buildService(resourceIndexHtml: String = "/webapp/index.html"): Service[IO] = {
    val ec = ExecutionContext.global
    new Service[IO](resourceIndexHtml, ec)(Effect[IO], Sync[IO], IO.contextShift(ec))
  }

}
