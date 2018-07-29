package org.mauritania.botinobe.api.v1

import cats.Monoid
import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.{HttpService, MediaType, Request, Response}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.mauritania.botinobe.Repository
import org.mauritania.botinobe.api.v1.Service.{CountResponse, IdResponse, DevicesResponse}
import org.mauritania.botinobe.models._
import org.mauritania.botinobe.models.Device.Metadata
import org.http4s.headers.`Content-Type`
import fs2.Stream
import org.mauritania.botinobe.helpers.Time

// Guidelines for REST:
// - https://blog.octo.com/wp-content/uploads/2014/10/RESTful-API-design-OCTO-Quick-Reference-Card-2.2.pdf

class Service(repository: Repository) extends Http4sDsl[IO] {

  val MinDeviceNameLength = 4

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
       |
       | // About reports (set by the device, read by the user)
       |
       | POST /devices/<device_name>/reports
       |
       | GET  /devices/<device_name>/reports/<id>
       |
       | GET  /devices/<device_name>/reports/last
       |
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
            p <- req.decodeJson[ActorMap]
            id <- repository.createTarget(Device(Metadata(None, Status.Created, device, Some(Time.now)), p))
            resp <- Created(IdResponse(id).asJson)
          } yield (resp)
        }
      }

      case req@GET -> Root / "devices" / device / "targets" :? CounMatcher(count) +& CleanMatcher(clean) +& MergeMatcher(merge) => {
        val targetIds = repository.readTargetIdsWhereStatus(device, Status.Created)
        if (count.exists(identity)) {
          Ok(readCountTargets(targetIds), `Content-Type`(MediaType.`application/json`))
        } else {
					Ok(
						readTargets(device, targetIds, clean.exists(identity), merge.exists(identity)),
						`Content-Type`(MediaType.`application/json`)
					)
				}
      }

      case req@GET -> Root / "devices" / device / "targets" / LongVar(id) => {
        for {
          t <- repository.readTarget(id)
          resp <- Ok(t.asJson)
        } yield (resp)
      }

      case req@GET -> Root / "devices" / device / "targets" / "last" => {
        for {
          r <- repository.readLastTarget(device)
          resp <- Ok(r.asJson)
        } yield (resp)
      }


      // Reports

      case req@GET -> Root / "devices" / device / "reports" / LongVar(id) => {
        for {
          r <- repository.readReport(id)
          resp <- Ok(r.asJson)
        } yield (resp)
      }

      case req@GET -> Root / "devices" / device / "reports" / "last" => {
        for {
          r <- repository.readLastReport(device)
          resp <- Ok(r.asJson)
        } yield (resp)
      }

      case req@POST -> Root / "devices" / device / "reports" => {
        if (device.length < MinDeviceNameLength) {
          ExpectationFailed(s"Device name lenght must be at least $MinDeviceNameLength")
        } else {
          for {
            p <- req.decodeJson[ActorMap]
            id <- repository.createReport(Device(Metadata(None, Status.Created, device, Some(Time.now)), p))
            resp <- Created(IdResponse(id).asJson)
          } yield (resp)
        }
      }

    }
  }

	private def readTargets(
		device: String,
		targetIds: Stream[IO, RecordId],
		clean: Boolean,
		merge: Boolean
	) = {
		val targets = for {
			id <- targetIds
			target <- Stream.eval[IO, Device](if (clean) repository.readTargetConsume(id) else repository.readTarget(id))
		} yield (target)
		val v = targets.fold(List.empty[Device])(_ :+ _).map(
			if (merge) Device.merge(device, Status.Created, _) else identity
		)
		v.map(DevicesResponse(_).asJson)
	}

	private def readCountTargets(targetIds: Stream[IO, RecordId]) = {
		targetIds.map(_ => 1).reduce(_ + _).lastOr(0).map(CountResponse(_).asJson)
	}

	private[v1] def request(r: Request[IO]): IO[Response[IO]] = service.orNotFound(r)

}

object Service {

  case class IdResponse(id: RecordId)
  case class CountResponse(count: Int)
  case class DevicesResponse(ts: Seq[Device])

}
