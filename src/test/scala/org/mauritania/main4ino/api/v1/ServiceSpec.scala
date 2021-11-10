package org.mauritania.main4ino.api.v1

import java.nio.file.Paths
import java.time._
import java.time.zone.ZoneRulesException

import cats._
import cats.effect.{IO, _}
import doobie.util.transactor.Transactor
import fs2.Stream
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.headers.Authorization
import org.http4s.{Request, Response, Uri, Status => HttpStatus, _}
import org.mauritania.main4ino.db.Repository.{FromTo, ReqType}
import org.mauritania.main4ino.db.Repository.ReqType.ReqType
import org.mauritania.main4ino.api.Translator
import org.mauritania.main4ino.api.Translator.TimeResponse
import org.mauritania.main4ino.helpers.Time
import org.mauritania.main4ino.models.Device.Metadata
import org.mauritania.main4ino.models.ForTestRicherClasses._
import org.mauritania.main4ino.models.{Device, EpochSecTimestamp}
import org.mauritania.main4ino.security.{Auther, User, Config => SecurityConfig}
import org.mauritania.main4ino.{Fixtures, Helper}
import org.scalamock.scalatest.MockFactory
import org.scalatest.EitherValues._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.mauritania.main4ino.DecodersIO
import org.mauritania.main4ino.db.Config.DbSyntax
import org.mauritania.main4ino.db.Repository
import org.mauritania.main4ino.firmware.Store
import org.mauritania.main4ino.firmware.{Service => FirmwareService}
import org.mauritania.main4ino.devicelogs.{Logger, Config => DevLogsConfig}
import org.mauritania.main4ino.models.Device.Metadata.Status
import org.scalatest.ParallelTestExecution

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ServiceSpec extends AnyWordSpec with MockFactory with Matchers with DecodersIO with Http4sDsl[IO] with ParallelTestExecution {

  val User1 = Fixtures.User1
  val User1Pass = Fixtures.User1Pass
  val ValidToken = "012345678901234567890123456789"
  val PrivateKey = "0123456789abcdef0123"
  val AuthConfig = SecurityConfig(List(User1), PrivateKey)
  val Dev1 = Fixtures.Device1
  val DevId1 = Fixtures.DeviceId1
  val DevId2 = Fixtures.DeviceId1.withId(2L).withDeviceName("dev2")
  val Dev1V1 = Fixtures.DeviceId1
  val Dev2V1 = Dev1V1.withDeviceName("dev2").withId(2L)

  implicit val statusEncoder = JsonEncoding.StatusEncoder
  implicit val statusDecoder = JsonEncoding.StatusDecoder

  class RepositoryIO(tr: Transactor[IO]) extends Repository[IO](DbSyntax.H2, tr)
  class TimeIO extends Time[IO]

  "Help request" should {
    val r = stub[RepositoryIO]
    val t = stub[TimeIO]
    val s = defaultService(r, t)
    "return 200" in {
      val q = getApiV1("/help")(s).unsafeRunSync()
      q.status shouldBe (HttpStatus.Ok)
      q.body.compile.toVector.unsafeRunSync().map(_.toChar).mkString should include("See: ")
    }
  }

  "Version request" should {
    val r = stub[RepositoryIO]
    val t = stub[TimeIO]
    val s = defaultService(r, t)
    "return 200" in {
      val q = getApiV1("/version")(s).unsafeRunSync()
      q.status shouldBe (HttpStatus.Ok)
      q.body.compile.toVector.unsafeRunSync().map(_.toChar).mkString should include("version")
    }
  }

  "Create target request" should {
    val r = stub[RepositoryIO]
    val t = stub[TimeIO]
    val s = defaultService(r, t)
    "return 201 with empty properties" in {
      (t.nowUtc _).when().returns(IO.pure(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))).anyNumberOfTimes() // mock
      val d = Device(Metadata("dev1", Metadata.Status.Closed))
      createADeviceAndExpect(ReqType.Reports, d)(HttpStatus.Created)(s, r)
      createADeviceAndExpect(ReqType.Targets, d)(HttpStatus.Created)(s, r)
    }
    "return 201 with a regular target/request" in {
      (t.nowUtc _).when().returns(IO.pure(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))).anyNumberOfTimes() // mock
      val d = Dev1
      createADeviceAndExpect(ReqType.Reports, d)(HttpStatus.Created)(s, r)
      createADeviceAndExpect(ReqType.Targets, d)(HttpStatus.Created)(s, r)
    }
  }

  private[this] def createADeviceAndExpect(
    t: ReqType,
    d: Device
  )(
    s: HttpStatus
  )(service: Service[IO], repository: Repository[IO]) = {
    (repository.insertDevice _).when(
      argThat[ReqType]("Addresses target table")(_ == t),
      argThat[Device]("Is the expected device")(_ => true),
      argThat[EpochSecTimestamp]("Is the expected timestamp")(_ => true)
    ).returns(IO.pure(1L)) // mock
    val body = Helper.asEntityBody[IO](d.actors.asJson.toString)
    postApiV1(s"/devices/${d.metadata.device}/${t.code}", body)(service).unsafeRunSync().status shouldBe (s)
  }


  "The service" should {
    val r = stub[RepositoryIO]
    val t = stub[TimeIO]
    val s = defaultService(r, t)
    "return 200 with an existent target/request when reading target/report requests" in {
      (r.selectDeviceWhereRequestId _).when(ReqType.Targets, Dev1.metadata.device, 1L).returns(IO.pure(Right(DevId1))).once // mock
      (r.selectDeviceWhereRequestId _).when(ReqType.Reports, Dev1.metadata.device, 1L).returns(IO.pure(Right(DevId1))).once() // mock
      val ta = getApiV1("/devices/dev1/targets/1")(s).unsafeRunSync()
      ta.status shouldBe (HttpStatus.Ok)
      ta.as[Json](Sync[IO], DecoderIOJson).unsafeRunSync() shouldBe (Dev1V1.asJson)

      val re = getApiV1("/devices/dev1/reports/1")(s).unsafeRunSync()
      re.status shouldBe (HttpStatus.Ok)
      re.as[Json](Sync[IO], DecoderIOJson).unsafeRunSync() shouldBe (Dev1V1.asJson)
    }
  }

  it should {
    val r = stub[RepositoryIO]
    val t = stub[TimeIO]
    val s = defaultService(r, t)
    "return 200 with a list of existent targets when reading all targets request" in {
      (r.selectDevicesWhereTimestampStatus _).when(ReqType.Targets, "dev1", FromTo(None, None), None).returns(IO.pure(Iterable(DevId1, DevId2))).once // mock

      val ta = getApiV1("/devices/dev1/targets")(s).unsafeRunSync()
      ta.status shouldBe (HttpStatus.Ok)
      ta.as[Json](Sync[IO], DecoderIOJson).unsafeRunSync() shouldBe (List(Dev1V1, Dev2V1).asJson)
    }
    "reject invalid statuses when reading reading all targets request" in {
      val ta = getApiV1("/devices/dev1/targets?status=UNEXISTENT_STATUS")(s).unsafeRunSync()
      ta.status shouldBe (HttpStatus.NotFound)
    }
  }

  private def defaultService(r: Repository[IO], t: Time[IO]) = {
    implicit val cs = IO.contextShift(Helper.testExecutionContext)
    val store = new Store[IO](basePath = Paths.get("/tmp"))
    val firmwareService = new FirmwareService(store, Helper.testExecutionContext)
    new Service(
      auth = new Auther[IO](AuthConfig),
      tr = new Translator[IO](
        repository = r,
        time = t,
        devLogger = new Logger[IO](
          DevLogsConfig(Paths.get("/tmp")),
          time = t,
          ExecutionContext.global
        )(Sync[IO], Concurrent[IO], cs, IO.timer(Helper.testExecutionContext)),
        store = store
      ),
      time = t,
      firmwareService
    )(Sync[IO])
  }

  it should {
    val r = stub[RepositoryIO]
    val t = stub[TimeIO]
    val s = defaultService(r, t)
    "return 400 when invalid timezone provided" in {
      (t.nowUtc _).when().returns(IO.pure(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))).once // mock

      val ta = getApiV1("/time?timezone=EuropeTOs/PariTOs")(s).unsafeRunSync() // unexistent timezone
      ta.status shouldBe (HttpStatus.BadRequest)
    }
    "return 200 with the time (default UTC timezone)" in {
      (t.nowUtc _).when().returns(IO.pure(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))).once // mock

      val ta = getApiV1("/time")(s).unsafeRunSync()
      ta.status shouldBe (HttpStatus.Ok)
      ta.as[Json](Sync[IO], DecoderIOJson).unsafeRunSync() shouldBe (TimeResponse("UTC", 0L, "1970-01-01T00:00:00").asJson)
    }
    "return 200 with the time (custom timezone)" in {
      (t.nowUtc _).when().returns(IO.pure(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))).once // mock

      val ta = getApiV1("/time?timezone=Europe/Paris")(s).unsafeRunSync()
      ta.status shouldBe (HttpStatus.Ok)
      ta.as[Json](Sync[IO], DecoderIOJson).unsafeRunSync() shouldBe (TimeResponse("Europe/Paris", 0, "1970-01-01T01:00:00").asJson)
    }
  }

  it should {

    val r = stub[RepositoryIO]
    val t = stub[TimeIO]
    val s = defaultService(r, t)

    "when invalid token" should {
      val r = stub[RepositoryIO]
      val t = stub[TimeIO]
      val s = defaultService(r, t)
      "return 403 (forbidden) if invalid credentials" in {
        getApiV1("/help", HeadersCredsInvalid)(s).unsafeRunSync().status shouldBe (HttpStatus.Forbidden)
      }
      "return 403 (forbidden) if no credentials" in {
        getApiV1("/help", HeadersNoCreds)(s).unsafeRunSync().status shouldBe (HttpStatus.Forbidden)
      }
      "return 403 (forbidden) if wrong credentials" in {
        getApiV1("/help", HeadersCredsWrong)(s).unsafeRunSync().status shouldBe (HttpStatus.Forbidden)
      }
      "return 200 if correct credentials (via headers)" in {
        getApiV1("/help", HeadersCredsOk)(s).unsafeRunSync().status shouldBe (HttpStatus.Ok)
      }
      "return 200 if correct credentials (via uri)" in {
        val token = BasicCredsOk.token
        getApiV1(s"http://localhost:3030/token/$token/help", HeadersNoCreds)(s).unsafeRunSync().status shouldBe (HttpStatus.Ok)
      }
    }

  }

  def e[T: ClassTag](v: T) = argThat[T](s"Expected value $v")(_ == v)

  // Basic testing utilities

  private[this] def getApiV1(path: String, h: Headers = HeadersCredsOk)(service: Service[IO]): IO[Response[IO]] = {
    val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(path), headers = h)
    req(service, request)
  }

  private[this] def postApiV1(path: String, body: EntityBody[IO], h: Headers = HeadersCredsOk)(service: Service[IO]): IO[Response[IO]] = {
    val request = Request[IO](method = Method.POST, uri = Uri.unsafeFromString(path), body = body, headers = h)
    req(service, request)
  }

  private[this] def req(service: Service[IO], request: Request[IO]) = {
    service.serviceWithAuthentication(request).getOrElseF(NotFound())
  }

  final val HeadersNoCreds = Headers.empty
  final val BasicCredsOk = BasicCredentials(User1.id, User1Pass)
  final val HeadersCredsOk = Headers.of(Authorization(BasicCredsOk))
  final val HeadersCredsWrong = Headers.of(Authorization(BasicCredentials(User1.id, "incorrectpassword")))
  final val HeadersCredsInvalid = Headers.of(Authorization(BasicCredentials(User1.id, "short")))

}
