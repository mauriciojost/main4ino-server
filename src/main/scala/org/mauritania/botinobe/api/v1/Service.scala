package org.mauritania.botinobe.api.v1

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.{HttpService, MediaType, Request, Response}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.mauritania.botinobe.Repository
import org.mauritania.botinobe.api.v1.Service.{CountResponse, DevicesResponse, IdResponse}
import org.mauritania.botinobe.models._
import org.mauritania.botinobe.models.Device.Metadata
import org.http4s.headers.`Content-Type`
import fs2.Stream
import org.mauritania.botinobe.api.v1.DeviceU.{ActorMapU, MetadataU}
import org.mauritania.botinobe.helpers.Time

// Guidelines for REST:
// - https://blog.octo.com/wp-content/uploads/2014/10/RESTful-API-design-OCTO-Quick-Reference-Card-2.2.pdf

class Service(repository: Repository) extends Http4sDsl[IO] {

  val MinDeviceNameLength = 4

  object CreatedMatcher extends OptionalQueryParamDecoderMatcher[Boolean]("created")
  object CounMatcher extends OptionalQueryParamDecoderMatcher[Boolean]("count")
  object MergeMatcher extends OptionalQueryParamDecoderMatcher[Boolean]("merge")
  object CleanMatcher extends OptionalQueryParamDecoderMatcher[Boolean]("clean")

  val HelpMsg = // TODO: complete me
    s"""
       | API HELP
       | --- ----
       |
       | // About help
       |
       | GET /help
       |
       |
       | // About targets (set by the user, read by device)
       |
       | POST /devices/<device_name>/targets
       |
       | GET  /devices/<device_name>/targets/<id>
       |
       | GET  /devices/<device_name>/targets/last
       |
       | GET  /devices/<device_name>/targets  params: count[boolean], clean[boolean], merge[boolean]
       |
       | GET  /devices/<device_name>/actors/<actor_name>/targets/
       |
       |
       | // About reports (set by the device, read by the user)
       |
       | POST /devices/<device_name>/reports
       |
       | GET  /devices/<device_name>/reports/<id>
       |
       | GET  /devices/<device_name>/reports/last
       |
       | POST /devices/<device_name>/actors/<actor_name>/reports
       | ...
       |
    """.stripMargin

  val service = {
    HttpService[IO] {


      // Help

      case GET -> Root / "help" => {
        Ok(HelpMsg)
      }


      // Targets

      case req@POST -> Root / "devices" / device / "targets" => {
        if (device.length < MinDeviceNameLength) {
          ExpectationFailed(s"Device name lenght must be at least $MinDeviceNameLength")
        } else {
          for {
            p <- req.decodeJson[ActorMapU]
            id <- repository.createTarget(DeviceU(MetadataU(None, Some(Time.now), device), p).toBom)
            resp <- Created(IdResponse(id).asJson)
          } yield (resp)
        }
      }

      case req@GET -> Root / "devices" / device / "targets" :? CreatedMatcher(createdOp) +& CounMatcher(countOp) +& CleanMatcher(cleanOp) +& MergeMatcher(mergeOp) => {

        val created = createdOp.exists(identity)
        val count = countOp.exists(identity)
        val clean = cleanOp.exists(identity)
        val merge = mergeOp.exists(identity)

        val propIds = repository.readTargetPropIdsWhereDeviceStatus(device, if (created) Some(Status.Created) else None)
        if (count) {
          Ok(readCountIds(propIds), `Content-Type`(MediaType.`application/json`))
        } else {
					Ok(readTargets(device, propIds, clean, merge), `Content-Type`(MediaType.`application/json`))
				}
      }

      case req@GET -> Root / "devices" / device / "targets" / LongVar(id) => {
        for {
          t <- repository.readTarget(id)
          resp <- Ok(DeviceU.fromBom(t).asJson)
        } yield (resp)
      }

      case req@GET -> Root / "devices" / device / "targets" / "last" => {
        for {
          r <- repository.readLastTarget(device)
          resp <- Ok(DeviceU.fromBom(r).asJson)
        } yield (resp)
      }

      case req@GET -> Root / "devices" / device / "actors" / actor / "targets" :? CreatedMatcher(createdOp) +& CounMatcher(countOp) +& CleanMatcher(cleanOp) +& MergeMatcher(mergeOp) => {

        val created = createdOp.exists(identity)
        val count = countOp.exists(identity)
        val clean = cleanOp.exists(identity)
        val merge = mergeOp.exists(identity)

        val propIds = repository.readTargetPropIdsWhereDeviceActorStatus(device, actor, if (created) Some(Status.Created) else None)
        if (count) {
          Ok(readCountIds(propIds), `Content-Type`(MediaType.`application/json`))
        } else {
					Ok(readTargets(device, propIds, clean, merge), `Content-Type`(MediaType.`application/json`))
				}
      }



      // Reports

      case req@GET -> Root / "devices" / device / "reports" / LongVar(id) => {
        for {
          r <- repository.readReport(id)
          resp <- Ok(DeviceU.fromBom(r).asJson)
        } yield (resp)
      }

      case req@GET -> Root / "devices" / device / "reports" / "last" => {
        for {
          r <- repository.readLastReport(device)
          resp <- Ok(DeviceU.fromBom(r).asJson)
        } yield (resp)
      }

      case req@POST -> Root / "devices" / device / "reports" => {
        if (device.length < MinDeviceNameLength) {
          ExpectationFailed(s"Device name lenght must be at least $MinDeviceNameLength")
        } else {
          for {
            p <- req.decodeJson[ActorMapU]
            id <- repository.createReport(DeviceU(MetadataU(None, Some(Time.now), device), p).toBom)
            resp <- Created(IdResponse(id).asJson)
          } yield (resp)
        }
      }

      case req@POST -> Root / "devices" / device / "actors" / actor / "targets" => {
        ???
        if (device.length < MinDeviceNameLength) {
          ExpectationFailed(s"Device name lenght must be at least $MinDeviceNameLength")
        } else {
          for {
            p <- req.decodeJson[ActorMapU]
            id <- repository.createTarget(DeviceU(MetadataU(None, Some(Time.now), device), p).toBom)
            resp <- Created(IdResponse(id).asJson)
          } yield (resp)
        }
      }

    }
  }

	private def readTargets(
		device: String,
		propIds: Stream[IO, RecordId],
		clean: Boolean,
		merge: Boolean
	) = {
		val targets = for {
      pd <- propIds
      target <- Stream.eval[IO, Device](if (clean) repository.readTargetConsume(pd) else repository.readTarget(pd))
		} yield (target)
		val v = targets.fold(List.empty[Device])(_ :+ _).map(
			if (merge) Device.merge else identity
		)
		v.map(i => DevicesResponse(i.map(DeviceU.fromBom)).asJson)
	}

	private def readCountIds(ids: Stream[IO, RecordId]) = {
		ids.map(_ => 1).reduce(_ + _).lastOr(0).map(CountResponse(_).asJson)
	}

	private[v1] def request(r: Request[IO]): IO[Response[IO]] = service.orNotFound(r)

}

object Service {

  case class IdResponse(id: RecordId)
  case class CountResponse(count: Int)
  case class DevicesResponse(response: Seq[DeviceU])

}
