package org.mauritania.main4ino.webapp

import cats.effect.{Effect, IO, Sync}
import org.http4s.{Method, Request, Response, Status, Uri}
import org.scalatest.{BeforeAndAfterEach}
import org.http4s.dsl.Http4sDsl

import scala.concurrent.ExecutionContext
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ServiceFuncSpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach with Http4sDsl[IO] {

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

  it should "fail to read non-existent non-app.js" in {
    get("/js/non-app.js")(buildService()).status shouldBe Status.NotFound
  }

  private[this] def getExpectOk(path: String)(service: Service[IO]): String = {
    val r = get(path)(service)
    r.status shouldBe Status.Ok
    r.bodyAsText.compile.toList.unsafeRunSync().mkString("\n")
  }

  private[this] def get(path: String)(service: Service[IO]): Response[IO] = {
    val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(path))
    service.service(request).getOrElseF(NotFound()).unsafeRunSync()
  }

  private def buildService(resourceIndexHtml: String = "/webapp/index.html"): Service[IO] = {
    val ec = ExecutionContext.global
    new Service[IO](resourceIndexHtml, ec)(Effect[IO], Sync[IO], IO.contextShift(ec))
  }

}
