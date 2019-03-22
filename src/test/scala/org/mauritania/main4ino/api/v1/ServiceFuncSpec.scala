package org.mauritania.main4ino.api.v1

import cats.effect.IO
import fs2.Stream
import io.circe.Json
import org.mauritania.main4ino.api.v1.Service.{CountResponse, IdResponse}
import io.circe.syntax._
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.headers.Authorization
import org.http4s.{BasicCredentials, EntityBody, Headers, Method, Request, Response, Status, Uri}
import org.mauritania.main4ino.api.v1.Service.{CountResponse, IdResponse}
import org.mauritania.main4ino.helpers.TimeIO
import org.mauritania.main4ino.security.Fixtures._
import org.mauritania.main4ino.security._
import org.mauritania.main4ino.{DbSuite, RepositoryIO}
import org.scalatest.Sequential

class ServiceFuncSpec extends DbSuite {

  Sequential

  "The service" should "create, read a target/report and delete it" in {

    implicit val s = new Service(new AuthenticationIO(DefaultSecurityConfig), new RepositoryIO(transactor), new TimeIO())

    // Add a few targets
    postExpectCreated("/devices/dev1/targets", """{"clock": {"h":"7"}}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces
    postExpectCreated("/devices/dev1/targets", """{"clock": {"m":"0"}}""").noSpaces shouldBe IdResponse(2).asJson.noSpaces
    postExpectCreated("/devices/dev1/targets", """{"clock": {"s":"1"}}""").noSpaces shouldBe IdResponse(3).asJson.noSpaces
    postExpectCreated("/devices/dev1/targets", """{"body": {"mv0":"Zz."}}""").noSpaces shouldBe IdResponse(4).asJson.noSpaces
    postExpectCreated("/devices/dev1/targets", """{"body": {"mv0":"ZzY"}}""").noSpaces shouldBe IdResponse(5).asJson.noSpaces
    postExpectCreated("/devices/dev2/targets", """{"body": {"mv0":"Zz."}}""").noSpaces shouldBe IdResponse(6).asJson.noSpaces // dev2
    postExpectCreated("/devices/dev1/targets", """{"clock": {"s":"9"}}""").noSpaces shouldBe IdResponse(7).asJson.noSpaces

    // Check the responses

    // Only the count of targets for dev1
    val dev1TargetsCount = getExpectOk("/devices/dev1/targets/count?status=C")
    dev1TargetsCount.noSpaces shouldBe CountResponse(6).asJson.noSpaces

    // Only the count of targets for dev1 clock
    val dev1ClockTargetsCount = getExpectOk("/devices/dev1/actors/clock/targets/count?status=C")
    dev1ClockTargetsCount.noSpaces shouldBe CountResponse(4).asJson.noSpaces

    // The raw targets list for dev1 / clock
    val dev1ClockTarget = getExpectOk("/devices/dev1/actors/clock/targets?status=C&consume=false")
    dev1ClockTarget.asArray.get(0).noSpaces shouldBe ("""{"h":"7"}""")
    dev1ClockTarget.asArray.get(1).noSpaces shouldBe ("""{"m":"0"}""")
    dev1ClockTarget.asArray.get(2).noSpaces shouldBe ("""{"s":"1"}""")
    dev1ClockTarget.asArray.get(3).noSpaces shouldBe ("""{"s":"9"}""")

    val dev1ClockBody = getExpectOk("/devices/dev1/actors/body/targets?status=C&consume=false")
    dev1ClockBody.asArray.get(0).noSpaces shouldBe ("""{"mv0":"Zz."}""")
    dev1ClockBody.asArray.get(1).noSpaces shouldBe ("""{"mv0":"ZzY"}""")

    // The merged targets for dev1
    val dev1TargetsSummaryBody = getExpectOk("/devices/dev1/actors/body/targets/summary?consume=false&status=C")
    dev1TargetsSummaryBody.noSpaces shouldBe """{"mv0":"ZzY"}"""

    val dev1TargetsSummaryClock = getExpectOk("/devices/dev1/actors/clock/targets/summary?consume=false&status=C")
    dev1TargetsSummaryClock.noSpaces shouldBe """{"h":"7","s":"9","m":"0"}"""

    val dev1TargetsMergedCleanedBody = getExpectOk("/devices/dev1/actors/body/targets/summary?consume=true&status=C")
    dev1TargetsMergedCleanedBody.noSpaces shouldBe """{"mv0":"ZzY"}"""

    val dev1TargetsMergedCleanedClock = getExpectOk("/devices/dev1/actors/clock/targets/summary?consume=true&status=C")
    dev1TargetsMergedCleanedClock.noSpaces shouldBe """{"h":"7","s":"9","m":"0"}"""

    val dev1TargetsCountAfterClean = getExpectOk("/devices/dev1/actors/body/targets/count?status=C")
    dev1TargetsCountAfterClean.noSpaces shouldBe CountResponse(0).asJson.noSpaces

    val dev1TargetsCountAnyStatus = getExpectOk("/devices/dev1/actors/body/targets/count")
    dev1TargetsCountAnyStatus.noSpaces shouldBe CountResponse(2).asJson.noSpaces

    delete("/administrator/devices/dev1/targets")

    val dev1TargetsCountAnyStatusAfter = getExpectOk("/devices/dev1/actors/body/targets/count")
    dev1TargetsCountAnyStatusAfter.noSpaces shouldBe CountResponse(0).asJson.noSpaces


  }

  it should "create and read a target/report in different value formats (string, int, bool)" in {

    implicit val s = new Service(new AuthenticationIO(DefaultSecurityConfig), new RepositoryIO(transactor), new TimeIO())

    // Add a target
    postExpectCreated("/devices/dev1/targets", """{"act":{"i":7,"b":true,"s":"str"}}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces

    // Check the responses
    val dev1 = getExpectOk("/devices/dev1/targets/1")
    dev1.\\("actors")(0).noSpaces shouldBe ("""{"act":{"b":"true","s":"str","i":"7"}}""")

  }

  it should "create targets and merge the properties correctly" in {

    implicit val s = new Service(new AuthenticationIO(DefaultSecurityConfig), new RepositoryIO(transactor), new TimeIO())

    // Add a few targets
    postExpectCreated("/devices/dev1/targets", """{"clock":{"h":"7"},"body":{"mv0":"Zz."}}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces
    postExpectCreated("/devices/dev1/targets", """{"clock":{"m":"0"}}""").noSpaces shouldBe IdResponse(2).asJson.noSpaces
    postExpectCreated("/devices/dev1/targets", """{"body":{"mv1":"Zz."}}""").noSpaces shouldBe IdResponse(3).asJson.noSpaces

    // Check the responses
    val dev1 = getExpectOk("/devices/dev1/targets/summary?consume=false&status=C")
    val clk = getExpectOk("/devices/dev1/actors/clock/targets/summary?consume=false&status=C")
    val body = getExpectOk("/devices/dev1/actors/body/targets/summary?consume=false&status=C")

    dev1.noSpaces shouldBe """{"body":{"mv1":"Zz.","mv0":"Zz."},"clock":{"h":"7","m":"0"}}"""
    clk.noSpaces shouldBe """{"h":"7","m":"0"}"""
    body.noSpaces shouldBe """{"mv1":"Zz.","mv0":"Zz."}"""

  }

  it should "retrieve correctly last device and actor views per status" in {

    implicit val s = new Service(new AuthenticationIO(DefaultSecurityConfig), new RepositoryIO(transactor), new TimeIO())

    // Add a few targets
    postExpectCreated("/devices/dev1/targets", """{"clock":{"h":"7"},"body":{"mv0":"Zz."}}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces
    postExpectCreated("/devices/dev1/targets", """{"clock":{"m":"0"}}""").noSpaces shouldBe IdResponse(2).asJson.noSpaces
    postExpectCreated("/devices/dev1/targets", """{"body":{"mv1":"Zz."}}""").noSpaces shouldBe IdResponse(3).asJson.noSpaces

    // Check the responses
    val clk = getExpectOk("/devices/dev1/actors/clock/targets/last?status=C")
    val body = getExpectOk("/devices/dev1/actors/body/targets/last?status=C")
    val dev1 = getExpectOk("/devices/dev1/targets/last?status=C")

    clk.noSpaces shouldBe """{"m":"0"}"""
    body.noSpaces shouldBe """{"mv1":"Zz."}"""
    dev1.\\("actors")(0).noSpaces shouldBe """{"body":{"mv1":"Zz."}}"""

  }

  it should "respond with no expectation failed when no records are found" in {

    implicit val s = new Service(new AuthenticationIO(DefaultSecurityConfig), new RepositoryIO(transactor), new TimeIO())

    get("/devices/dev1/targets/1").status shouldBe Status.NoContent
    get("/devices/dev1/targets/last").status shouldBe Status.NoContent
    get("/devices/dev1/targets/summary").status shouldBe Status.NoContent
    get("/devices/dev1/actors/clock/targets/last").status shouldBe Status.NoContent
    get("/devices/dev1/actors/body/targets/last").status shouldBe Status.NoContent
    get("/devices/dev1/actors/body/targets/summary").status shouldBe Status.NoContent

  }

  it should "create targets properties and merge them correctly" in {

    implicit val s = new Service(new AuthenticationIO(DefaultSecurityConfig), new RepositoryIO(transactor), new TimeIO())

    // Add a few targets
    postExpectCreated("/devices/dev1/actors/body/targets", """{"mv0":"Zz."}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces
    postExpectCreated("/devices/dev1/actors/body/targets", """{"mv1":"0"}""").noSpaces shouldBe IdResponse(2).asJson.noSpaces
    postExpectCreated("/devices/dev1/actors/body/targets", """{"mv1":"Zz.","mv2":"Da2"}""").noSpaces shouldBe IdResponse(3).asJson.noSpaces

    // Check the responses
    val body = getExpectOk("/devices/dev1/actors/body/targets/summary?status=C")

    body.noSpaces shouldBe """{"mv2":"Da2","mv1":"Zz.","mv0":"Zz."}"""

  }

  private[this] def getExpectOk(path: String)(implicit service: Service[IO]): Json = {
    val r = get(path)
    r.status shouldBe Status.Ok
    r.as[Json].unsafeRunSync()
  }

  private[this] def get(path: String)(implicit service: Service[IO]): Response[IO] = {
    val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(path), headers = DefaultHeaders)
    service.request(request).unsafeRunSync()
  }

  private[this] def postExpectCreated(path: String, body: String)(implicit service: Service[IO]): Json = {
    val r = post(path, body)
    r.status shouldBe Status.Created
    r.as[Json].unsafeRunSync()
  }

  private[this] def post(path: String, body: String)(implicit service: Service[IO]): Response[IO] = {
    val request = Request[IO](method = Method.POST, uri = Uri.unsafeFromString(path), body = asEntityBody(body), headers = DefaultHeaders)
    service.request(request).unsafeRunSync()
  }

  private[this] def delete(path: String)(implicit service: Service[IO]): Response[IO] = {
    val request = Request[IO](method = Method.DELETE, uri = Uri.unsafeFromString(path), headers = DefaultHeaders)
    service.request(request).unsafeRunSync()
  }

  private[this] def asEntityBody(content: String): EntityBody[IO] = {
    Stream.fromIterator[IO, Byte](content.toCharArray.map(_.toByte).toIterator)
  }

  final val DefaultHeaders = Headers(Authorization(BasicCredentials(User1.id, User1Pass)))

}
