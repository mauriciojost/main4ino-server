package org.mauritania.main4ino.api.v1

import java.nio.file.{Paths, Path => NioPath}
import java.time.{Instant, ZoneId, ZonedDateTime}

import cats.effect.{IO, Sync}
import doobie.hikari.HikariTransactor
import fs2.Stream
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.headers.Authorization
import org.http4s.{Status, _}
import org.mauritania.main4ino.api.Translator
import org.mauritania.main4ino.api.Translator.{CountResponse, IdResponse, IdsOnlyResponse}
import org.mauritania.main4ino.db.{Repository, TransactorCtx}
import org.mauritania.main4ino.firmware.Store
import org.mauritania.main4ino.helpers.Time
import org.mauritania.main4ino.models.Description
import org.mauritania.main4ino.models.Description.VersionJson
import org.mauritania.main4ino.security.Fixtures._
import org.mauritania.main4ino.security._
import org.mauritania.main4ino.{Helper, TmpDirCtx}
import org.mauritania.main4ino.firmware.{Service => FirmwareService}
import cats._
import cats.implicits._
import org.http4s.dsl.Http4sDsl
import org.mauritania.main4ino.devicelogs.{Config, Logger}
import org.scalatest.ParallelTestExecution
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ServiceFuncSpec extends AnyFlatSpec with Matchers with TransactorCtx with TmpDirCtx with Http4sDsl[IO] with ParallelTestExecution {

  val TheTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))

  class FixedTime extends Time[IO] {
    override def nowUtc: IO[ZonedDateTime] = IO.pure(TheTime)
  }

  def defaultServiceWithDirectory(transactor: HikariTransactor[IO], tmp: NioPath): Service[IO] = {
    val t = new FixedTime()
    implicit val cs = IO.contextShift(Helper.testExecutionContext)
    new Service(
      new Auther(DefaultSecurityConfig),
      new Translator(
        new Repository(transactor),
        t,
        new Logger[IO](Config(tmp), t, Helper.testExecutionContext)(Sync[IO], cs)
      ),
      t,
      new FirmwareService[IO](new Store(Paths.get("src/test/resources/firmwares/1")), Helper.testExecutionContext)
    )
  }

  def defaultService(transactor: HikariTransactor[IO]): Service[IO] = {
    val tmp = Paths.get("/tmp/")
    defaultServiceWithDirectory(transactor, tmp)
  }

  implicit val statusEncoder = JsonEncoding.StatusEncoder
  implicit val statusDecoder = JsonEncoding.StatusDecoder
  implicit val stringDecoder = JsonEncoding.StringDecoder


  "The service from web ui" should "create and delete devices" in {
    withTransactor { tr =>
      implicit val s = defaultService(tr)

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

  }

  it should "generate a session and tell the user currently logged in and its managed devices" in {
    withTransactor { tr =>
      implicit val ds = defaultService(tr)

      val s = post("/session", "")
      s.status shouldBe Status.Ok
      val session = s.as[String].unsafeRunSync()
      session.length should be > 128
      session.length should be < 256

      val u = get("/user")
      u.status shouldBe Status.Ok
      u.as[String].unsafeRunSync() shouldBe User1.name

      val d = get("/devices")
      d.status shouldBe Status.Ok
      d.as[String].unsafeRunSync() shouldBe "[]"
    }

  }


  it should "create, read a target" in {

    withTransactor { tr =>
      implicit val s = defaultService(tr)

      // Add a multi-actor target
      postExpectCreated("/devices/dev1/targets", """{"clock":{"h":"7"},"speaker":{"v":"0"}}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces

      // Check the responses

      // Ids existent for dev1 with status C (closed)
      val dev1TargetsCount = getExpectOk("/devices/dev1/targets?ids=true&status=C")
      dev1TargetsCount.noSpaces shouldBe IdsOnlyResponse(List(1)).asJson.noSpaces

    }

  }

  "The service from the device" should "create and read a target/report in different value formats (string, int, bool)" in {
    withTransactor { tr =>
      implicit val s = defaultService(tr)

      // Add a target
      postExpectCreated("/devices/dev1/targets", """{"act":{"i":7,"b":true,"s":"str"}}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces

      // Check the responses
      val dev1 = getExpectOk("/devices/dev1/targets/1")
      dev1.\\("actors")(0).noSpaces shouldBe ("""{"act":{"b":"true","s":"str","i":"7"}}""")
    }

  }

  it should "list available firmwares" in {
    withTransactor { tr =>
      implicit val s = defaultService(tr)
      val r0 = get("/devices/dev1/firmware/firmwares/botino/esp8266")
      r0.status shouldBe Status.Ok
      val r1 = get("/token/234234234234/devi1ces/dev1/firmware/firmwares/botino/esp8266", Headers.empty)
      r1.status shouldBe Status.Forbidden
      val r2 = get(s"/token/${DefaultCredentials.token}/devices/dev1/firmware/firmwares/botino/esp8266", Headers.empty)
      r2.status shouldBe Status.Ok
      val r3 = get("/devices/dev1/firmware/xxx")
      r3.status shouldBe Status.NotFound
    }
  }

  it should "create targets and merge the properties correctly" in {
    withTransactor { tr =>

      implicit val s = defaultService(tr)

      // Add a few targets
      postExpectCreated("/devices/dev1/targets", """{"clock":{"h":"7"},"body":{"mv0":"Zz."}}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces
      postExpectCreated("/devices/dev1/targets", """{"clock":{"m":"0"}}""").noSpaces shouldBe IdResponse(2).asJson.noSpaces
      postExpectCreated("/devices/dev1/targets", """{"body":{"mv1":"Zz."}}""").noSpaces shouldBe IdResponse(3).asJson.noSpaces

      // Check the responses
      val dev1 = getExpectOk("/devices/dev1/targets/summary?status=C&newstatus=X")
      dev1.noSpaces shouldBe """{"body":{"mv1":"Zz.","mv0":"Zz."},"clock":{"h":"7","m":"0"}}"""

      // Check the responses after commit
      val dev1b = get("/devices/dev1/targets/summary?status=C")
      dev1b.status shouldBe Status.NoContent

      // Check the responses requesting invalid commit
      val dev1c = get("/devices/dev1/targets/summary?status=X&newstatus=C")
      dev1c.status shouldBe Status.ExpectationFailed

      postExpectCreated("/devices/dev1/targets", """{"body":{"mv2":"Zz."}}""").noSpaces shouldBe IdResponse(4).asJson.noSpaces

      // Check the responses requesting invalid transition (newly created device snapshot should not be changed)
      val dev1d = get("/devices/dev1/targets/summary?status=C&newstatus=O")
      dev1d.status shouldBe Status.ExpectationFailed

      // Check the responses after commit (should have one snapshot in status C)
      val dev1e = get("/devices/dev1/targets/summary?status=C")
      dev1e.status shouldBe Status.Ok
    }

  }

  it should "retrieve correctly last device view per status" in {
    withTransactor { tr =>

      implicit val s = defaultService(tr)

      // Add a few targets
      postExpectCreated("/devices/dev1/targets", """{"clock":{"h":"7"},"body":{"mv0":"Zz."}}""").noSpaces shouldBe IdResponse(1).asJson.noSpaces
      postExpectCreated("/devices/dev1/targets", """{"clock":{"m":"0"}}""").noSpaces shouldBe IdResponse(2).asJson.noSpaces
      postExpectCreated("/devices/dev1/targets", """{"body":{"mv1":"Zz."}}""").noSpaces shouldBe IdResponse(3).asJson.noSpaces

      // Check the responses
      val dev1 = getExpectOk("/devices/dev1/targets/last?status=C")

      dev1.\\("actors")(0).noSpaces shouldBe """{"body":{"mv1":"Zz."}}"""
    }

  }

  it should "respond with no expectation failed when no records are found" in {
    withTransactor { tr =>

      implicit val s = defaultService(tr)

      get("/devices/dev1/targets/1").status shouldBe Status.NoContent
      get("/devices/dev1/targets/last").status shouldBe Status.NoContent
      get("/devices/dev1/targets/summary").status shouldBe Status.NoContent
    }

  }

  it should "store and read logs coming from a device" in {
    withTransactor { tr =>
      withTmpDir { tmp =>
        implicit val s = defaultServiceWithDirectory(tr, tmp)
        val logMsg = "failure\nhere"
        putExpect("/devices/dev1/logs", logMsg, Status.Ok)

        val log = get(s"/devices/dev1/logs?from=0&to=1")
        log.status should be(Status.Ok)
        log.bodyText.compile.toList.unsafeRunSync().toList should be(List("0 failure", "\n", "0 here"))
      }
    }
  }

  it should "fail gracefully if logs do not exist" in {
    withTransactor { tr =>
      withTmpDir { tmp =>
        implicit val s = defaultServiceWithDirectory(tr, tmp)
        val log = get(s"/devices/dev1/logs?from=0&to=0")
        log.status should be(Status.Ok)
      }
    }
  }

  "The service from both web ui and device" should "create and read descriptions by device name" in {
    withTransactor { tr =>
      implicit val s = defaultService(tr)

      val r0 = get("/devices/dev1/descriptions")
      r0.status shouldBe Status.NoContent

      putExpectOk("/devices/dev1/descriptions", """{"version": "1.0.0", "json":null}""").noSpaces shouldBe CountResponse(1).asJson.noSpaces

      getExpectOk("/devices/dev1/descriptions").noSpaces shouldBe Description("dev1", Time.asTimestamp(TheTime), VersionJson("1.0.0", Json.Null)).asJson.noSpaces

      putExpectOk("/devices/dev1/descriptions", """{"version": "1.1.0", "json":null}""").noSpaces shouldBe CountResponse(1).asJson.noSpaces

      getExpectOk("/devices/dev1/descriptions").noSpaces shouldBe Description("dev1", Time.asTimestamp(TheTime), VersionJson("1.1.0", Json.Null)).asJson.noSpaces
    }
  }

  private[this] def getExpectOk(path: String)(implicit service: Service[IO]): Json = {
    val r = get(path)
    r.status shouldBe Status.Ok
    r.as[Json].unsafeRunSync()
  }

  private[this] def get(path: String, headers: Headers = DefaultHeaders)(implicit service: Service[IO]): Response[IO] = {
    val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(path), headers = headers)
    req(service, request).unsafeRunSync()
  }

  private[this] def postExpectCreated(path: String, body: String)(implicit service: Service[IO]): Json = {
    val r = post(path, body)
    r.status shouldBe Status.Created
    r.as[Json].unsafeRunSync()
  }

  private[this] def putExpectOk(path: String, body: String)(implicit service: Service[IO]): Json = {
    val r = put(path, body)
    r.status shouldBe Status.Ok
    r.as[Json].unsafeRunSync()
  }

  private[this] def putExpect(path: String, body: String, status: Status)(implicit service: Service[IO]): Unit = {
    val r = put(path, body)
    r.status shouldBe status
  }

  private[this] def put(path: String, body: String)(implicit service: Service[IO]): Response[IO] = {
    val request = Request[IO](method = Method.PUT, uri = Uri.unsafeFromString(path), body = asEntityBody(body), headers = DefaultHeaders)
    req(service, request).unsafeRunSync()
  }

  private[this] def post(path: String, body: String)(implicit service: Service[IO]): Response[IO] = {
    val request = Request[IO](method = Method.POST, uri = Uri.unsafeFromString(path), body = asEntityBody(body), headers = DefaultHeaders)
    req(service, request).unsafeRunSync()
  }

  private[this] def delete(path: String)(implicit service: Service[IO]): Response[IO] = {
    val request = Request[IO](method = Method.DELETE, uri = Uri.unsafeFromString(path), headers = DefaultHeaders)
    req(service, request).unsafeRunSync()
  }

  private[this] def req(service: Service[IO], request: Request[IO]) = {
    service.serviceWithAuthentication(request).getOrElseF(NotFound())
  }

  private[this] def asEntityBody(content: String): EntityBody[IO] = {
    Stream.fromIterator[IO](content.toCharArray.map(_.toByte).toIterator)
  }

  final val DefaultCredentials = BasicCredentials(User1.id, User1Pass)
  final val DefaultHeaders = Headers(Authorization(DefaultCredentials))

}
