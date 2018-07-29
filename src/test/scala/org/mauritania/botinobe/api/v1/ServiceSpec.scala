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
import org.mauritania.botinobe.Repository
import org.mauritania.botinobe.models.Device.Metadata
import org.mauritania.botinobe.models.{RecordId, Device, Status => MStatus}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

class ServiceSpec extends WordSpec with MockFactory with Matchers {

  val DeviceFixt = Device(Metadata(None, MStatus.Created, "dev1", Some(0L)), Map("actor1" -> Map("prop1" -> "value1")))

  "Help request" should {

    val r = stub[Repository]
    val s = new Service(r)

    "return 200" in {
      getApiV1( "/help")(s).status shouldBe(HttpStatus.Ok)
    }

    "return help message" in {
      getApiV1( "/help")(s).as[String].unsafeRunSync() should include("API HELP")
    }

  }


  "Create target request" should {

    val r = stub[Repository]
    val s = new Service(r)

    "returns 201 with empty properties" in {
      val t = Device(Metadata(None, MStatus.Created, "dev1", Some(0L)))
      testATargetCreationReturns(t)(HttpStatus.Created)(s, r)
    }

    "returns 201 with a regular target" in {
      val t = DeviceFixt
      testATargetCreationReturns(t)(HttpStatus.Created)(s, r)
    }

    "returns 417 with an empty device name" in {
      val t = Device(Metadata(None, MStatus.Created, "", Some(0L)))
      testATargetCreationReturns(t)(HttpStatus.ExpectationFailed)(s, r)
    }

  }

  private [this] def testATargetCreationReturns(
    t: Device
  )(
    s: HttpStatus
  )(service: Service, repository: Repository) = {
    (repository.createTarget _).when(*).returns(IO.pure(1L)) // mock
    val body = asEntityBody(t.actors.asJson.toString)
    postApiV1(s"/devices/${t.metadata.device}/targets", body)(service).status shouldBe(s)
  }


  "Read target request" should {

    val r = stub[Repository]
    val s = new Service(r)

    "returns 200 with an existent target" in {
      testATargetReadReturns(1L, DeviceFixt)(HttpStatus.Ok, DeviceFixt.asJson)(s, r)
    }
  }

  private [this] def testATargetReadReturns(
    id: RecordId,
    t: Device
  )(
    s: HttpStatus,
    body: Json
  )(service: Service, repository: Repository) = {
    (repository.readTarget _).when(id).returns(IO.pure(t)) // mock
    getApiV1("/devices/dev1/targets/1")(service).status shouldBe(HttpStatus.Ok)
    getApiV1("/devices/dev1/targets/1")(service).as[Json].unsafeRunSync() shouldBe(DeviceFixt.asJson)
  }

  // Basic testing utilities

  private[this] def getApiV1(path: String)(service: Service): Response[IO] = {
    val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(path))
    service.request(request).unsafeRunSync()
  }

  private[this] def postApiV1(path: String, body: EntityBody[IO])(service: Service): Response[IO] = {
    val request = Request[IO](method = Method.POST, uri = Uri.unsafeFromString(path), body = body)
    service.request(request).unsafeRunSync()
  }

  private [this] def asEntityBody(content: String): EntityBody[IO] = {
    Stream.fromIterator[IO, Byte](content.toCharArray.map(_.toByte).toIterator)
  }

  /*
  |
  | // create a 2 targets for dev1 and 1 for dev2
  | >  POST /v1/devices/<dev1>/targets/  {"actor1":{"prop1": "val1"}}
  | <    200 {"id": 1}
  | >  POST /v1/devices/<dev1>/targets/  {"actor1":{"prop2": "val2"}}
  | <    200 {"id": 2}
  | >  POST /v1/devices/<dev2>/targets/  {"actor3":{"prop3": "val3"}, "actor4": {"prop4": "val4"}}
  | <    200 {"id": 3}
  | >  POST /v1/devices/<dev2>/targets/  {"actor3":{"prop5": "val5"}}
  | <    200 {"id": 4}
  |
  | // existent targets for dev1
  | >  GET /v1/devices/<dev1>/targets/
  | <    200 {"targets":[1, 2], "count": 2}
  |
  | // get all targets merged dev1 and clean (transactional)
  | >  GET /v1/devices/<dev1>/targets?merge=true&clean=true
  | <    200 {"actor1":{"prop1": "val1", "prop2": "val2"}}
  |
  | // now there are no remaining targets to be consumed
  | >  GET /v1/devices/<dev1>/targets/
  | <    200 {"targets":[], "count": 0}
  |
  | // we can retrieve a specific target
  | >  GET /v1/devices/<dev2>/targets/<3>
  | <    200 {"actor3": {"prop3": "val3"}, "actor4": {"prop4": "val4"}}
  |
  | >  GET /v1/devices/<dev2>/actors/actor3/targets?merge=true&clean=true
  | <    200 {"prop5": "val5"}
  |
  */

}
