package org.mauritania.botinobe.api.v1

import cats.effect.IO
import org.http4s._
import org.http4s.implicits._
import org.specs2.matcher.MatchResult
import cats.effect.IO
import fs2.Stream
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.{HttpService, MediaType, Request, Response}
import org.http4s.circe._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.dsl.Http4sDsl._
import org.http4s.syntax._
import org.http4s.{Request, Response, Uri, Status => HttpStatus}
import org.mauritania.botinobe.Repository.Table
import org.mauritania.botinobe.Repository.Table.Table
import org.mauritania.botinobe.api.v1.DeviceU.MetadataU
import org.mauritania.botinobe.{Fixtures, Repository}
import org.mauritania.botinobe.models.Device.Metadata
import org.mauritania.botinobe.models.{ActorTup, Device, RecordId, Status => S}
import org.mauritania.botinobe.security.{Authentication, Config, User}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import org.mauritania.botinobe.models.RicherBom._

import scala.reflect.ClassTag

class ServiceSpec extends WordSpec with MockFactory with Matchers {

  val Token = "012345678901234567890123456789"
  val AuthConfig = Config(List(User(1, "name", "name@gmail.com", List("/"), Token)))
  val Dev1 = Fixtures.Device1
  val Dev2 = Fixtures.Device1.withId(Some(2L)).withDeviceName("dev2")
  val Dev1V1 = Fixtures.Device1InV1
  val Dev2V1 = Dev1V1.copy(metadata = Dev1V1.metadata.copy(id = Some(2L), device = "dev2"))

  "Help request" should {
    val r = stub[Repository]
    val s = new Service(new Authentication(AuthConfig), r)
    "return 200" in {
      getApiV1("/help")(s).status shouldBe (HttpStatus.Ok)
    }
    "return help message" in {
      getApiV1("/help")(s).as[String].unsafeRunSync() should include("API HELP")
    }
  }


  "Create target request" should {
    val r = stub[Repository]
    val s = new Service(new Authentication(AuthConfig), r)
    "return 201 with empty properties" in {
      val t = Device(Metadata(None, None, "dev1"))
      createADeviceAndExpect(Table.Reports, t)(HttpStatus.Created)(s, r)
      createADeviceAndExpect(Table.Targets, t)(HttpStatus.Created)(s, r)
    }
    "return 201 with a regular target/request" in {
      val t = Dev1
      createADeviceAndExpect(Table.Reports, t)(HttpStatus.Created)(s, r)
      createADeviceAndExpect(Table.Targets, t)(HttpStatus.Created)(s, r)
    }
  }

  private[this] def createADeviceAndExpect(
    t: Table,
    d: Device
  )(
    s: HttpStatus
  )(service: Service, repository: Repository) = {
    (repository.insertDevice _).when(
      argThat[Table]("Addresses target table")(_ == t),
      argThat[Device]("Is the expected device")(x => x.withouIdNortTimestamp() == d.withouIdNortTimestamp())
    ).returns(IO.pure(1L)) // mock
    val body = asEntityBody(DeviceU.fromBom(d).actors.asJson.toString)
    postApiV1(s"/devices/${d.metadata.device}/${t.code}", body)(service).status shouldBe (s)
  }


  "The service" should {
    val r = stub[Repository]
    val s = new Service(new Authentication(AuthConfig), r)
    "return 200 with an existent target/request when reading target/report requests" in {
      (r.selectDeviceWhereRequestId _).when(Table.Targets, 1L).returns(IO.pure(Dev1)).once // mock
      (r.selectDeviceWhereRequestId _).when(Table.Reports, 1L).returns(IO.pure(Dev1)).once() // mock
      val ta = getApiV1("/devices/dev1/targets/1")(s)
      ta.status shouldBe (HttpStatus.Ok)
      ta.as[Json].unsafeRunSync() shouldBe (Dev1V1.asJson)

      val re = getApiV1("/devices/dev1/reports/1")(s)
      re.status shouldBe (HttpStatus.Ok)
      re.as[Json].unsafeRunSync() shouldBe (Dev1V1.asJson)
    }
  }

  it should {
    val r = stub[Repository]
    val s = new Service(new Authentication(AuthConfig), r)
    "return 200 with a list of existent targets when reading all targets request" in {
      (r.selectDevicesWhereTimestamp _).when(Table.Targets, "dev1", None, None).returns(Stream.fromIterator[IO, Device](Iterator(Dev1, Dev2))).once // mock

      val ta = getApiV1("/devices/dev1/targets")(s)
      ta.status shouldBe (HttpStatus.Ok)
      ta.as[Json].unsafeRunSync() shouldBe (List(Dev1V1, Dev2V1).asJson)
    }
  }

  it should {

    val r = stub[Repository]
    val s = new Service(new Authentication(AuthConfig), r)

    "return the list of associated targets set when merging correctly existent targets" in {

      (r.selectActorTupWhereDeviceActorStatus _)
        .when(e(Table.Targets), e("dev1"), e(Some("clock")), e(Some(S.Created)), e(false))
        .returns(Stream.fromIterator[IO, ActorTup](Iterator(
          ActorTup(Some(1), "dev1", "clock", "h", "7", S.Consumed),
          ActorTup(Some(2), "dev1", "clock", "h", "8", S.Consumed)
        ))).once()

      val r1 = s.getDevActors("dev1", "clock", Table.Targets, Some(S.Created), None)
      val r1m = r1.compile.toList.unsafeRunSync().head.toSet

      r1m shouldBe Set(
        Map("h" -> "7"),
        Map("h" -> "8")
      )
    }

    "return the actor tups for a given actor" in {
      val tups = List(
        ActorTup(Some(1), "dev1", "clock", "h", "7", S.Created),
        ActorTup(Some(2), "dev1", "clock", "m", "0", S.Created),
        ActorTup(Some(3), "dev1", "clock", "h", "8", S.Created)
      )
      (r.selectActorTupWhereDeviceActorStatus _)
        .when(e(Table.Targets), e("dev1"), e(Some("clock")), e(Some(S.Created)), false)
        .returns(Stream.fromIterator[IO, ActorTup](tups.toIterator)).once()

      val r2 = s.getDevActorTups("dev1", Some("clock"), Table.Targets, Some(S.Created), None)
      val r2m = r2.unsafeRunSync().toSet

      r2m shouldBe tups.toSet

    }

  }

  def e[T: ClassTag](v: T) = argThat[T](s"Expected value $v")(_ == v)

  // Basic testing utilities

  private[this] def getApiV1(path: String)(service: Service): Response[IO] = {
    val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(path), headers = DefaultHeaders)
    service.request(request).unsafeRunSync()
  }

  private[this] def postApiV1(path: String, body: EntityBody[IO])(service: Service): Response[IO] = {
    val request = Request[IO](method = Method.POST, uri = Uri.unsafeFromString(path), body = body, headers = DefaultHeaders)
    service.request(request).unsafeRunSync()
  }

  private[this] def asEntityBody(content: String): EntityBody[IO] = {
    Stream.fromIterator[IO, Byte](content.toCharArray.map(_.toByte).toIterator)
  }

  final val DefaultHeaders = Headers(Header("Authorization", "token " + Token))

}
