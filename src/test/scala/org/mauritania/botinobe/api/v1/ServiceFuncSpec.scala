package org.mauritania.botinobe.api.v1

import cats.effect.IO
import org.mauritania.botinobe.{DbSuite, Repository}
import org.http4s.{EntityBody, HttpService, MediaType, Method, Request, Response, Uri}
import fs2.Stream
import io.circe.Json
import org.mauritania.botinobe.api.v1.Service.{CountResponse, IdResponse}
import io.circe.syntax._
import org.http4s.circe._
import io.circe.generic.auto._

class ServiceFuncSpec extends DbSuite {

  /*
  "The service" should "create and read a target/report" in {

    implicit val s = new Service(new Repository(transactor))

    // Add a few targets
    post("/devices/dev1/targets", """{"clock": {"h":"7"}}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces
    post("/devices/dev1/targets", """{"clock": {"m":"0"}}""").noSpaces shouldBe IdResponse(2).asJson.noSpaces
    post("/devices/dev1/targets", """{"clock": {"s":"1"}}""").noSpaces shouldBe IdResponse(3).asJson.noSpaces
    post("/devices/dev1/targets", """{"body": {"mv0":"Zz."}}""").noSpaces shouldBe IdResponse(4).asJson.noSpaces
    post("/devices/dev1/targets", """{"body": {"mv0":"ZzY"}}""").noSpaces shouldBe IdResponse(5).asJson.noSpaces
    post("/devices/dev2/targets", """{"body": {"mv0":"Zz."}}""").noSpaces shouldBe IdResponse(6).asJson.noSpaces // dev2
    post("/devices/dev1/targets", """{"clock": {"s":"9"}}""").noSpaces shouldBe IdResponse(7).asJson.noSpaces

    // Check the responses

    // Only the count of targets for dev1
    val dev1TargetsCount = get("/devices/dev1/targets?clean=false&merge=false&count=true")
    dev1TargetsCount.noSpaces shouldBe CountResponse(6).asJson.noSpaces

    // The raw targets list for dev1 / clock
    val dev1ClockTarget = get("/devices/dev1/actors/clock/targets?clean=false&merge=false&count=false")
    dev1ClockTarget.\\("response")(0).\\("actors")(0).\\("clock").head.noSpaces shouldBe("""{"h":"7"}""")
    dev1ClockTarget.\\("response")(0).\\("actors")(1).\\("clock").head.noSpaces shouldBe("""{"m":"0"}""")
    dev1ClockTarget.\\("response")(0).\\("actors")(2).\\("clock").head.noSpaces shouldBe("""{"s":"1"}""")
    dev1ClockTarget.\\("response")(0).\\("actors")(3).\\("clock").head.noSpaces shouldBe("""{"s":"9"}""")

    val dev1ClockBody = get("/devices/dev1/actors/body/targets?clean=false&merge=false&count=false")
    dev1ClockBody.\\("response")(0).\\("actors")(0).\\("body").head.noSpaces shouldBe("""{"mv0":"Zz."}""")
    dev1ClockBody.\\("response")(0).\\("actors")(1).\\("body").head.noSpaces shouldBe("""{"mv0":"ZzY"}""")

    // The merged targets for dev1
    val dev1TargetsMerged = get("/devices/dev1/targets?clean=false&merge=true&count=false&created=true")
    dev1TargetsMerged.\\("response")(0).\\("actors")(0).\\("body").head.noSpaces shouldBe """{"mv0":"ZzY"}"""
    dev1TargetsMerged.\\("response")(0).\\("actors")(0).\\("clock").head.noSpaces shouldBe """{"h":"7","s":"9","m":"0"}"""

    val dev1TargetsMergedCleaned = get("/devices/dev1/targets?clean=true&merge=true&count=false&created=true")
    dev1TargetsMergedCleaned.\\("response")(0).\\("actors")(0).\\("body").head.noSpaces shouldBe """{"mv0":"ZzY"}"""
    dev1TargetsMergedCleaned.\\("response")(0).\\("actors")(0).\\("clock").head.noSpaces shouldBe """{"h":"7","s":"9","m":"0"}"""

    val dev1TargetsCountAfterClean = get("/devices/dev1/targets?clean=false&merge=false&count=true&created=true")
    dev1TargetsCountAfterClean.noSpaces shouldBe CountResponse(0).asJson.noSpaces

  }
  */

  it should "create targets and merge the properties correctly" in {

    implicit val s = new Service(new Repository(transactor))

    // Add a few targets
    post("/devices/dev1/targets", """{"clock":{"h":"7"},"body":{"mv0":"Zz."}}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces
    post("/devices/dev1/targets", """{"clock":{"m":"0"}}""").noSpaces shouldBe IdResponse(2).asJson.noSpaces
    post("/devices/dev1/targets", """{"body":{"mv1":"Zz."}}""").noSpaces shouldBe IdResponse(3).asJson.noSpaces

    // Check the responses
    val clk = get("/devices/dev1/actors/clock/targets?clean=false&merge=true&count=false&created=true")
    val body = get("/devices/dev1/actors/body/targets?clean=false&merge=true&count=false&created=true")
    clk.\\("response")(0).\\("actors")(0).\\("clock").head.noSpaces shouldBe """{"h":"7","m":"0"}"""
    clk.\\("response")(0).\\("actors")(0).\\("body").head.noSpaces shouldBe """{}"""
    body.\\("response")(0).\\("actors")(0).\\("body").head.noSpaces shouldBe """{"mv1":"Zz.","mv0":"Zz."}"""
    body.\\("response")(0).\\("actors")(0).\\("clock").head.noSpaces shouldBe """{}"""

  }

  private[this] def get(path: String)(implicit service: Service): Json = {
    val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(path))
    service.request(request).unsafeRunSync().as[Json].unsafeRunSync()
  }

  private[this] def post(path: String, body: String)(implicit service: Service): Json = {
    val request = Request[IO](method = Method.POST, uri = Uri.unsafeFromString(path), body = asEntityBody(body))
    service.request(request).unsafeRunSync().as[Json].unsafeRunSync()
  }

  private [this] def asEntityBody(content: String): EntityBody[IO] = {
    Stream.fromIterator[IO, Byte](content.toCharArray.map(_.toByte).toIterator)
  }

}
