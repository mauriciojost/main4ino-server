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
import org.mauritania.botinobe.{Fixtures, Repository}
import org.mauritania.botinobe.models.Device.Metadata
import org.mauritania.botinobe.models.{Device, RecordId, Status => MStatus}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

class ServiceSpec extends WordSpec with MockFactory with Matchers {

  val Dev1 = Fixtures.Device1

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
      val t = Device(Metadata(None, Some(0L), "dev1"))
      createATargetAndExpect(t)(HttpStatus.Created)(s, r)
    }

    "returns 201 with a regular target" in {
      val t = Dev1
      createATargetAndExpect(t)(HttpStatus.Created)(s, r)
    }

    "returns 417 with an empty device name" in {
      val t = Device(Metadata(None, Some(0L), ""))
      createATargetAndExpect(t)(HttpStatus.ExpectationFailed)(s, r)
    }

  }

  private [this] def createATargetAndExpect(
    d: Device
  )(
    s: HttpStatus
  )(service: Service, repository: Repository) = {
    (repository.createDevice _).when(*, *).returns(IO.pure(1L)) // mock
    val body = asEntityBody(DeviceU.fromBom(d).actors.asJson.toString)
    postApiV1(s"/devices/${d.metadata.device}/targets", body)(service).status shouldBe(s)
  }


  "Read target request" should {

    val r = stub[Repository]
    val s = new Service(r)

    "returns 200 with an existent target" in {
      (r.readDevice _).when(*, 1L).returns(IO.pure(Dev1)) // mock
      getApiV1("/devices/dev1/targets/1")(s).status shouldBe(HttpStatus.Ok)
      getApiV1("/devices/dev1/targets/1")(s).as[Json].unsafeRunSync() shouldBe(Dev1.asJson)
    }
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

}
