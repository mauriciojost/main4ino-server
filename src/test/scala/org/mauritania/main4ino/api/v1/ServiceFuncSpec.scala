package org.mauritania.main4ino.api.v1

import java.time.{Instant, ZoneId, ZonedDateTime}

import cats.effect.IO
import fs2.Stream
import io.circe.{Encoder, Json}
import io.circe.syntax._
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.headers.Authorization
import org.http4s.{BasicCredentials, EntityBody, Headers, Method, Request, Response, Status, Uri}
import org.mauritania.main4ino.api.Translator
import org.mauritania.main4ino.api.Translator.{CountResponse, IdResponse, IdsOnlyResponse}
import org.mauritania.main4ino.helpers.{Time, TimeIO}
import org.mauritania.main4ino.models.Description
import org.mauritania.main4ino.models.Description.VersionJson
import org.mauritania.main4ino.models.Device.Metadata
import org.mauritania.main4ino.security.Fixtures._
import org.mauritania.main4ino.security._
import org.mauritania.main4ino.{DbSuite, RepositoryIO}
import org.scalatest.Sequential

class ServiceFuncSpec extends DbSuite {

  Sequential

  val TheTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))

  class FixedTimeIO extends Time[IO] {
    def nowUtc: IO[ZonedDateTime] = IO.pure(TheTime)
  }

  def defaultService: Service[IO] = {
    val t = new FixedTimeIO()
    new Service(new AutherIO(DefaultSecurityConfig), new Translator(new RepositoryIO(transactor), t), t)
  }

  implicit val statusEncoder = JsonEncoding.StatusEncoder
  implicit val statusDecoder = JsonEncoding.StatusDecoder
  implicit val stringDecoder = JsonEncoding.StringDecoder


  "The service from web ui" should "create and delete devices" in {

    implicit val s = defaultService

    // Add a target
    postExpectCreated("/devices/dev1/targets", """{"clock": {"h":"7"}}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces
    postExpectCreated("/devices/dev2/targets", """{"clock": {"h":"8"}}""").noSpaces shouldBe IdResponse(2).asJson.noSpaces

    // Only the count of targets for dev1
    getExpectOk("/devices/dev1/targets?ids=true").noSpaces shouldBe IdsOnlyResponse(List(1)).asJson.noSpaces

    // Only the count of targets for dev2
    getExpectOk("/devices/dev2/targets?ids=true").noSpaces shouldBe IdsOnlyResponse(List(2)).asJson.noSpaces

    delete("/administrator/devices/dev1/targets")

    // Only the count of targets for dev1
    getExpectOk("/devices/dev1/targets?ids=true").noSpaces shouldBe IdsOnlyResponse(List()).asJson.noSpaces // deleted

    // Only the count of targets for dev2
    getExpectOk("/devices/dev2/targets?ids=true").noSpaces shouldBe IdsOnlyResponse(List(2)).asJson.noSpaces

  }

  it should "generate a session and tell the user currently logged in" in {

    implicit val ds = defaultService

    val s = post("/session", "")
    s.status shouldBe Status.Ok
    s.as[String].unsafeRunSync() should include ("-" + User1.name)

    val u = get("/user")
    u.status shouldBe Status.Ok
    u.as[String].unsafeRunSync() shouldBe User1.name

  }


  it should "create, read a target" in {

    implicit val s = defaultService

    // Add a multi-actor target
    postExpectCreated("/devices/dev1/targets", """{"clock":{"h":"7"},"speaker":{"v":"0"}}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces

    // Check the responses

    // Ids existent for dev1 with status C (closed)
    val dev1TargetsCount = getExpectOk("/devices/dev1/targets?ids=true&status=C")
    dev1TargetsCount.noSpaces shouldBe IdsOnlyResponse(List(1)).asJson.noSpaces

    // Retrieve one actor
    val dev1ClockTarget = getExpectOk("/devices/dev1/targets/1/actors/clock")
    dev1ClockTarget.noSpaces shouldBe Map("h" -> "7").asJson.noSpaces

    // Retrieve another actor
    val dev1SpeakerTarget = getExpectOk("/devices/dev1/targets/1/actors/speaker")
    dev1SpeakerTarget.noSpaces shouldBe Map("v" -> "0").asJson.noSpaces

  }

  it should "create, read a target actor" in {

    implicit val s = defaultService

    // Add a multi-actor target
    postExpectCreated("/devices/dev1/targets/actors/clock", """{"h":"7"}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces

    // Check the responses

    // Ids existent for dev1 with status C (closed)
    val dev1TargetsCount = getExpectOk("/devices/dev1/targets?ids=true&status=C")
    dev1TargetsCount.noSpaces shouldBe IdsOnlyResponse(List(1)).asJson.noSpaces

    // Retrieve one actor
    val dev1ClockTarget = getExpectOk("/devices/dev1/targets/1/actors/clock")
    dev1ClockTarget.noSpaces shouldBe Map("h" -> "7").asJson.noSpaces

  }


  "The service from the device" should "create and read a target/report in different value formats (string, int, bool)" in {

    implicit val s = defaultService

    // Add a target
    postExpectCreated("/devices/dev1/targets", """{"act":{"i":7,"b":true,"s":"str"}}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces

    // Check the responses
    val dev1 = getExpectOk("/devices/dev1/targets/1")
    dev1.\\("actors")(0).noSpaces shouldBe ("""{"act":{"b":"true","s":"str","i":"7"}}""")

  }

  it should "create a target/report and fill it in afterwards" in {

    implicit val s = defaultService

    // Add a target (empty)
    postExpectCreated("/devices/dev1/targets", """{}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces

    getExpectOk("/devices/dev1/targets?ids=true").noSpaces shouldBe IdsOnlyResponse(List(1)).asJson.noSpaces // not until at least one property added

    postExpectCreated("/devices/dev1/targets/1/actors/actor1", """{"prop1":"val1"}""").noSpaces shouldBe CountResponse(1).asJson.noSpaces // inserted
    postExpectCreated("/devices/dev1/targets/1/actors/actor1", """{"prop1":"val11"}""").noSpaces shouldBe CountResponse(1).asJson.noSpaces // inserted after

    getExpectOk("/devices/dev1/targets?ids=true").noSpaces shouldBe IdsOnlyResponse(List(1)).asJson.noSpaces

    // Check the responses
    getExpectOk("/devices/dev1/targets/1").\\("actors")(0).noSpaces shouldBe ("""{"actor1":{"prop1":"val11"}}""")


    // The request is open
    getExpectOk("/devices/dev1/targets/1").\\("metadata")(0).\\("status")(0).asString shouldBe Some(Metadata.Status.Open.code)

    // Close the request
    putExpect("/devices/dev1/targets/1?status=C", "", Status.Ok)
    getExpectOk("/devices/dev1/targets/1").\\("metadata")(0).\\("status")(0).asString shouldBe Some(Metadata.Status.Closed.code)

    // Consume the request
    putExpect("/devices/dev1/targets/1?status=X", "", Status.Ok)
    getExpectOk("/devices/dev1/targets/1").\\("metadata")(0).\\("status")(0).asString shouldBe Some(Metadata.Status.Consumed.code)

    // Check its not anymore available if requesting for closed requests
    getExpectOk("/devices/dev1/targets?ids=true&status=C").noSpaces shouldBe IdsOnlyResponse(List()).asJson.noSpaces

    // Attempt to set status to open again (should be ignored as the transition is not allowed)
    putExpect("/devices/dev1/targets/1?status=O", "{}", Status.NotModified)

  }


  it should "create targets and merge the properties correctly" in {

    implicit val s = defaultService

    // Add a few targets
    postExpectCreated("/devices/dev1/targets", """{"clock":{"h":"7"},"body":{"mv0":"Zz."}}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces
    postExpectCreated("/devices/dev1/targets", """{"clock":{"m":"0"}}""").noSpaces shouldBe IdResponse(2).asJson.noSpaces
    postExpectCreated("/devices/dev1/targets", """{"body":{"mv1":"Zz."}}""").noSpaces shouldBe IdResponse(3).asJson.noSpaces

    // Check the responses
    val dev1 = getExpectOk("/devices/dev1/targets/summary?status=C")

    dev1.noSpaces shouldBe """{"body":{"mv1":"Zz.","mv0":"Zz."},"clock":{"h":"7","m":"0"}}"""

  }

  it should "retrieve correctly last device view per status" in {

    implicit val s = defaultService

    // Add a few targets
    postExpectCreated("/devices/dev1/targets", """{"clock":{"h":"7"},"body":{"mv0":"Zz."}}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces
    postExpectCreated("/devices/dev1/targets", """{"clock":{"m":"0"}}""").noSpaces shouldBe IdResponse(2).asJson.noSpaces
    postExpectCreated("/devices/dev1/targets", """{"body":{"mv1":"Zz."}}""").noSpaces shouldBe IdResponse(3).asJson.noSpaces

    // Check the responses
    val dev1 = getExpectOk("/devices/dev1/targets/last?status=C")

    dev1.\\("actors")(0).noSpaces shouldBe """{"body":{"mv1":"Zz."}}"""

  }

  it should "retrieve correctly last device view per actor and status" in {

    implicit val s = defaultService

    // Add a few targets
    postExpectCreated("/devices/dev1/targets", """{"clock":{"h":"7"}}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces
    postExpectCreated("/devices/dev1/targets", """{"body":{"mv1":"Zz."}}""").noSpaces shouldBe IdResponse(2).asJson.noSpaces

    // Check the responses
    val dev1a = getExpectOk("/devices/dev1/targets/actors/clock/last?status=C")
    dev1a.noSpaces shouldBe """{"h":"7"}"""

    val dev1b = getExpectOk("/devices/dev1/targets/actors/body/last?status=C")
    dev1b.noSpaces shouldBe """{"mv1":"Zz."}"""

  }


  it should "respond with no expectation failed when no records are found" in {

    implicit val s = defaultService

    get("/devices/dev1/targets/1").status shouldBe Status.NoContent
    get("/devices/dev1/targets/last").status shouldBe Status.NoContent
    get("/devices/dev1/targets/summary").status shouldBe Status.NoContent
    get("/devices/dev1/targets/actors/clock/last").status shouldBe Status.NoContent
    get("/devices/dev1/targets/actors/body/last").status shouldBe Status.NoContent

  }

  "The service from both" should "create and read description by device name" in {
    implicit val s = defaultService

    val r0 = get("/devices/dev1/descriptions")
    r0.status shouldBe Status.NoContent

    postExpectCreated("/devices/dev1/descriptions", """{"version": "1.0.0", "json":null}""").noSpaces shouldBe CountResponse(1).asJson.noSpaces

    getExpectOk("/devices/dev1/descriptions").noSpaces shouldBe Description("dev1", Time.asTimestamp(TheTime), VersionJson("1.0.0", Json.Null)).asJson.noSpaces

    postExpectCreated("/devices/dev1/descriptions", """{"version": "1.1.0", "json":null}""").noSpaces shouldBe CountResponse(1).asJson.noSpaces

    getExpectOk("/devices/dev1/descriptions").noSpaces shouldBe Description("dev1", Time.asTimestamp(TheTime), VersionJson("1.1.0", Json.Null)).asJson.noSpaces
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

  private[this] def putExpect(path: String, body: String, status: Status)(implicit service: Service[IO]): Unit = {
    val r = put(path, body)
    r.status shouldBe status
  }

  private[this] def put(path: String, body: String)(implicit service: Service[IO]): Response[IO] = {
    val request = Request[IO](method = Method.PUT, uri = Uri.unsafeFromString(path), body = asEntityBody(body), headers = DefaultHeaders)
    service.request(request).unsafeRunSync()
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
