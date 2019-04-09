package org.mauritania.main4ino.api.v1

import java.time._

import cats._
import fs2.Stream
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.headers.Authorization
import org.http4s.{Request, Response, Uri, Status => HttpStatus, _}
import org.mauritania.main4ino.Repository.Table
import org.mauritania.main4ino.Repository.Table.Table
import org.mauritania.main4ino.api.Translator
import org.mauritania.main4ino.api.Translator.TimeResponse
import org.mauritania.main4ino.helpers.Time
import org.mauritania.main4ino.models.Device.Metadata
import org.mauritania.main4ino.models.RicherBom._
import org.mauritania.main4ino.models.Device
import org.mauritania.main4ino.security.Authentication.{AccessAttempt, UserSession}
import org.mauritania.main4ino.security.{Authentication, Config, User}
import org.mauritania.main4ino.{Fixtures, Helper, Repository, SyncId}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

import scala.reflect.ClassTag

class ServiceSpec extends WordSpec with MockFactory with Matchers with SyncId {

  val User1 = Fixtures.User1
  val User1Pass = Fixtures.User1Pass
  val Salt = Fixtures.Salt
  val ValidToken = "012345678901234567890123456789"
  val PrivateKey = "0123456789abcdef0123"
  val AuthConfig = Config(List(User1), PrivateKey, Salt)
  val Dev1 = Fixtures.Device1
  val Dev2 = Fixtures.Device1.withId(Some(2L)).withDeviceName("dev2")
  val Dev1V1 = Fixtures.Device1
  val Dev2V1 = Dev1V1.copy(metadata = Dev1V1.metadata.copy(id = Some(2L), device = "dev2"))

  "Help request" should {
    val r = stub[Repository[Id]]
    val t = stub[Time[Id]]
    val s = new Service(new AuthenticationId(AuthConfig), new Translator(r, t), t)(SyncId)
    "return 200" in {
      getApiV1("/help")(s).status shouldBe (HttpStatus.Ok)
    }
    "return help message" in {
      getApiV1("/help")(s).as[String](SyncId, DecoderIdString) should include("API HELP")
    }
  }

  class AuthenticationId(config: Config) extends Authentication[Id] {
    def authenticateAndCheckAccessFromRequest(request: Request[Id]): Id[AccessAttempt] =
      Authentication.authenticateAndCheckAccess(config.usersBy, config.encryptionConfig, request.headers, request.uri, request.uri.path)

    def generateSession(user: User): Id[UserSession] =
      Authentication.sessionFromUser(user, config.privateKeyBits, config.nonceStartupTime)
  }

  "Create target request" should {
    val r = stub[Repository[Id]]
    val t = stub[Time[Id]]
    val s = new Service(new AuthenticationId(AuthConfig), new Translator(r, t), t)(SyncId)
    "return 201 with empty properties" in {
      (t.nowUtc _).when().returns(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)).twice // mock
      val d = Device(Metadata(None, None, "dev1", Metadata.Status.Closed))
      createADeviceAndExpect(Table.Reports, d)(HttpStatus.Created)(s, r)
      createADeviceAndExpect(Table.Targets, d)(HttpStatus.Created)(s, r)
    }
    "return 201 with a regular target/request" in {
      (t.nowUtc _).when().returns(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)).twice // mock
      val d = Dev1
      createADeviceAndExpect(Table.Reports, d)(HttpStatus.Created)(s, r)
      createADeviceAndExpect(Table.Targets, d)(HttpStatus.Created)(s, r)
    }
  }

  private[this] def createADeviceAndExpect(
    t: Table,
    d: Device
  )(
    s: HttpStatus
  )(service: Service[Id], repository: Repository[Id]) = {
    (repository.insertDevice _).when(
      argThat[Table]("Addresses target table")(_ == t),
      argThat[Device]("Is the expected device")(x => x.withouIdNortTimestamp() == d.withouIdNortTimestamp())
    ).returns(1L) // mock
    val body = Helper.asEntityBody(d.actors.asJson.toString)
    postApiV1(s"/devices/${d.metadata.device}/${t.code}", body)(service).status shouldBe (s)
  }


  "The service" should {
    val r = stub[Repository[Id]]
    val t = stub[Time[Id]]
    val s = new Service(new AuthenticationId(AuthConfig), new Translator(r, t), t)(SyncId)
    "return 200 with an existent target/request when reading target/report requests" in {
      (r.selectDeviceWhereRequestId _).when(Table.Targets, Dev1.metadata.device, 1L).returns(Right(Dev1)).once // mock
      (r.selectDeviceWhereRequestId _).when(Table.Reports, Dev1.metadata.device, 1L).returns(Right(Dev1)).once() // mock
      val ta = getApiV1("/devices/dev1/targets/1")(s)
      ta.status shouldBe (HttpStatus.Ok)
      ta.as[Json](SyncId, DecoderIdJson) shouldBe (Dev1V1.asJson)

      val re = getApiV1("/devices/dev1/reports/1")(s)
      re.status shouldBe (HttpStatus.Ok)
      re.as[Json](SyncId, DecoderIdJson) shouldBe (Dev1V1.asJson)
    }
  }

  it should {
    val r = stub[Repository[Id]]
    val t = stub[Time[Id]]
    val s = new Service(new AuthenticationId(AuthConfig), new Translator(r, t), t)(SyncId)
    "return 200 with a list of existent targets when reading all targets request" in {
      (r.selectDevicesWhereTimestampStatus _).when(Table.Targets, "dev1", None, None, None).returns(Iterable(Dev1, Dev2)).once // mock

      val ta = getApiV1("/devices/dev1/targets")(s)
      ta.status shouldBe (HttpStatus.Ok)
      ta.as[Json](SyncId, DecoderIdJson) shouldBe (List(Dev1V1, Dev2V1).asJson)
    }
  }

  it should {
    val r = stub[Repository[Id]]
    val t = stub[Time[Id]]
    val s = new Service(new AuthenticationId(AuthConfig), new Translator(r, t), t)(SyncId)
    "return 400 when invalid timezone provided" in {
      (t.nowUtc _).when().returns(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)).once // mock

      val ta = getApiV1("/time?timezone=EuropeTOs/PariTOs")(s) // unexistent timezone
      ta.status shouldBe (HttpStatus.BadRequest)
    }
    "return 200 with the time (default UTC timezone)" in {
      (t.nowUtc _).when().returns(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)).once // mock

      val ta = getApiV1("/time")(s)
      ta.status shouldBe (HttpStatus.Ok)
      ta.as[Json](SyncId, DecoderIdJson) shouldBe (TimeResponse("UTC", 0L, "1970-01-01T00:00:00").asJson)
    }
    "return 200 with the time (custom timezone)" in {
      (t.nowUtc _).when().returns(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)).once // mock

      val ta = getApiV1("/time?timezone=Europe/Paris")(s)
      ta.status shouldBe (HttpStatus.Ok)
      ta.as[Json](SyncId, DecoderIdJson) shouldBe (TimeResponse("Europe/Paris", 0, "1970-01-01T01:00:00").asJson)
    }
  }

  it should {

    val r = stub[Repository[Id]]
    val t = stub[Time[Id]]
    val s = new Service(new AuthenticationId(AuthConfig), new Translator(r, t), t)(SyncId)

    /*
    "return the list of associated targets set when merging correctly existent targets" in {

      (r.selectActorTupWhereDeviceActorStatus _)
        .when(e(Table.Targets), e("dev1"), e(Some("clock")), e(Some(S.Created)), e(false))
        .returns(Stream.fromIterator[Id, ActorTup](Iterator(
          ActorTup(Some(1), "dev1", "clock", "h", "7", S.Consumed, None),
          ActorTup(Some(2), "dev1", "clock", "h", "8", S.Consumed, None)
        ))).once()

      val r1 = s.getDevActors("dev1", "clock", Table.Targets, Some(S.Created), None)
      val r1m = r1.toSet

      r1m shouldBe Set(
        Map("h" -> "7"),
        Map("h" -> "8")
      )
    }

    "return the actor tups for a given actor" in {
      val tups = List(
        ActorTup(Some(1), "dev1", "clock", "h", "7", S.Created, None),
        ActorTup(Some(2), "dev1", "clock", "m", "0", S.Created, None),
        ActorTup(Some(3), "dev1", "clock", "h", "8", S.Created, None)
      )
      (r.selectActorTupWhereDeviceActorStatus _)
        .when(e(Table.Targets), e("dev1"), e(Some("clock")), e(Some(S.Created)), false)
        .returns(Stream.fromIterator[Id, ActorTup](tups.toIterator)).once()

      val r2 = s.getDevActorTups("dev1", Some("clock"), Table.Targets, Some(S.Created), None)
      val r2m = r2.toSet

      r2m shouldBe tups.toSet

    }
    */

    "when invalid token" should {
      val r = stub[Repository[Id]]
      val t = stub[Time[Id]]
      val s = new Service(new AuthenticationId(AuthConfig), new Translator(r, t), t)(SyncId)
      "return 403 (forbidden) if invalid credentials" in {
        getApiV1("/help", HeadersCredsInvalid)(s).status shouldBe (HttpStatus.Forbidden)
      }
      "return 403 (forbidden) if no credentials" in {
        getApiV1("/help", HeadersNoCreds)(s).status shouldBe (HttpStatus.Forbidden)
      }
      "return 403 (forbidden) if wrong credentials" in {
        getApiV1("/help", HeadersCredsWrong)(s).status shouldBe (HttpStatus.Forbidden)
      }
      "return 200 if correct credentials (via headers)" in {
        getApiV1("/help", HeadersCredsOk)(s).status shouldBe (HttpStatus.Ok)
      }
      "return 200 if correct credentials (via uri)" in {
        val token = BasicCredsOk.token
        getApiV1(s"http://localhost:3030/token/$token/help", HeadersNoCreds)(s).status shouldBe (HttpStatus.Ok)
      }
    }

  }

  def e[T: ClassTag](v: T) = argThat[T](s"Expected value $v")(_ == v)

  // Basic testing utilities

  private[this] def getApiV1(path: String, h: Headers = HeadersCredsOk)(service: Service[Id]): Response[Id] = {
    val request = Request[Id](method = Method.GET, uri = Uri.unsafeFromString(path), headers = h)
    service.request(request)
  }

  private[this] def postApiV1(path: String, body: EntityBody[Id], h: Headers = HeadersCredsOk)(service: Service[Id]): Response[Id] = {
    val request = Request[Id](method = Method.POST, uri = Uri.unsafeFromString(path), body = body, headers = h)
    service.request(request)
  }

  final val HeadersNoCreds = Headers()
  final val BasicCredsOk = BasicCredentials(User1.id, User1Pass)
  final val HeadersCredsOk = Headers(Authorization(BasicCredsOk))
  final val HeadersCredsWrong = Headers(Authorization(BasicCredentials(User1.id, "incorrectpassword")))
  final val HeadersCredsInvalid = Headers(Authorization(BasicCredentials(User1.id, "short")))

}
