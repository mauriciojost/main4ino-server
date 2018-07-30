package org.mauritania.botinobe.api.v1

import cats.effect.IO
import io.circe.syntax._
import org.http4s.circe._
import io.circe.generic.auto._
import org.http4s.{HttpService, MediaType, Request, Response}
import org.http4s.dsl.Http4sDsl
import org.mauritania.botinobe.Repository
import org.mauritania.botinobe.api.v1.Service.{CountResponse, DevicesResponse, IdResponse}
import org.mauritania.botinobe.models._
import org.http4s.headers.`Content-Type`
import fs2.Stream
import org.mauritania.botinobe.Repository.Table
import org.mauritania.botinobe.Repository.Table.Table
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

  object TableVar {
    def unapply(str: String): Option[Table] = Table.resolve(str)
  }

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
       | GET  /devices/<device_name>/targets                       params: created[boolean], count[boolean], clean[boolean], merge[boolean]
       |
       | GET  /devices/<device_name>/actors/<actor_name>/targets   params: created[boolean], count[boolean], clean[boolean], merge[boolean]
       |
       |
       | // About reports (set by the device, read by the user)
       |
       | POST /devices/<device_name>/reports                       params: created[boolean], count[boolean], clean[boolean], merge[boolean]
       |
       | GET  /devices/<device_name>/reports/<id>
       |
       | GET  /devices/<device_name>/reports/last
       |
       | POST /devices/<device_name>/actors/<actor_name>/reports  params: created[boolean], count[boolean], clean[boolean], merge[boolean]
       | ...
       |
    """.stripMargin

  val service = {
    HttpService[IO] {

      // Help

      case GET -> Root / "help" => {
        Ok(HelpMsg)
      }


      // Targets & Reports

      case req@POST -> Root / "devices" / device / TableVar(table) => {
        if (device.length < MinDeviceNameLength) {
          ExpectationFailed(s"Device name lenght must be at least $MinDeviceNameLength")
        } else {
          for {
            p <- req.decodeJson[ActorMapU]
            id <- repository.createDevice(table, DeviceU(MetadataU(None, Some(Time.now), device), p).toBom)
            resp <- Created(IdResponse(id).asJson)
          } yield (resp)
        }
      }

      /**
      NOT SUPPORTED
      case req@GET -> Root / "devices" / device / TableVar(table) :? CreatedMatcher(createdOp) +& CounMatcher(countOp) +& CleanMatcher(cleanOp) +& MergeMatcher(mergeOp) => {
        val created = createdOp.exists(identity)
        val count = countOp.exists(identity)
        val clean = cleanOp.exists(identity)
        val merge = mergeOp.exists(identity)
        val propIds = repository.readDevicePropIdsWhereDeviceStatus(table, device, if (created) Some(Status.Created) else None)
        if (count) {
          Ok(readCountIds(propIds), `Content-Type`(MediaType.`application/json`))
        } else {
					Ok(readDevicesFromPropIds(table, device, actor, status, propIds, clean, merge), `Content-Type`(MediaType.`application/json`))
				}
      }
      */

      case req@GET -> Root / "devices" / device / TableVar(table) / LongVar(id) => {
        for {
          t <- repository.readDevice(table, id)
          resp <- Ok(DeviceU.fromBom(t).asJson)
        } yield (resp)
      }

      case req@GET -> Root / "devices" / device / TableVar(table) / "last" => {
        for {
          r <- repository.readLastDevice(table, device)
          resp <- Ok(DeviceU.fromBom(r).asJson)
        } yield (resp)
      }

      case req@GET -> Root / "devices" / device / "actors" / actor / TableVar(table) :? CreatedMatcher(createdOp) +& CounMatcher(countOp) +& CleanMatcher(cleanOp) +& MergeMatcher(mergeOp) => {
        val created = createdOp.exists(identity)
        val count = countOp.exists(identity)
        val clean = cleanOp.exists(identity)
        val merge = mergeOp.exists(identity)
        val status = if (created) Status.Created else Status.Consumed
        val propIds = repository.readDevicePropIdsWhereDeviceActorStatus(table, device, actor, status)
        if (count) {
          Ok(readCountIds(propIds), `Content-Type`(MediaType.`application/json`))
        } else {
					Ok(readDevicesFromPropIds(table, device, actor, status, propIds, clean, merge), `Content-Type`(MediaType.`application/json`))
				}
      }

    }
  }

	private[v1] def readDevicesFromPropIds(
    table: Repository.Table.Table,
		device: DeviceName,
    actor: ActorName,
    status: Status,
		propIds: Stream[IO, RecordId],
		clean: Boolean,
		merge: Boolean
	) = {
		val targets = for {
      pis <- propIds
      target <- (if (clean) repository.readPropsConsume(table, device, actor, status) else repository.readProps(table, device, actor, status))
		} yield (target)
		val v = targets.fold(List.empty[Device])(_ :+ _).map(
			if (merge) Device.merge else identity
		)
		v.map(i => DevicesResponse(i.map(DeviceU.fromBom)).asJson)
	}

	private[v1] def readCountIds(ids: Stream[IO, RecordId]) = {
		ids.map(_ => 1).reduce(_ + _).lastOr(0).map(CountResponse(_).asJson)
	}

	private[v1] def request(r: Request[IO]): IO[Response[IO]] = service.orNotFound(r)

}

object Service {

  case class IdResponse(id: RecordId)
  case class CountResponse(count: Int)
  case class DevicesResponse(response: Seq[DeviceU])

}
