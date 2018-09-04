package org.mauritania.main4ino.api.v1

import io.circe.syntax._
import org.http4s.circe._
import io.circe.generic.auto._
import org.http4s.{HttpService, MediaType}
import org.mauritania.main4ino.Repository
import org.mauritania.main4ino.models._
import org.http4s.headers.`Content-Type`
import org.mauritania.main4ino.Repository.Table.Table
import org.mauritania.main4ino.api.v1.ActorMapU.ActorMapU
import org.mauritania.main4ino.api.v1.DeviceU.MetadataU
import org.mauritania.main4ino.api.v1.PropsMapU.PropsMapU
import org.mauritania.main4ino.helpers.Time
import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import fs2.Stream
import org.http4s.{AuthedService, Request, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.mauritania.main4ino.models.Device.Metadata
import org.mauritania.main4ino.security.{Authentication, User}

class Service(auth: Authentication, repository: Repository) extends Http4sDsl[IO] {

  import Service._
  import Url._

  val onFailure: AuthedService[String, IO] = Kleisli(req => OptionT.liftF(Forbidden(req.authInfo)))
  val middleware = AuthMiddleware(auth.authUser, onFailure)

  val HelpMsg = // TODO: complete me in scaladoc and expose
    s"""
       | API HELP
       | --- ----
       | GET /help
       |
       |    Display this help
       |
       |    Returns: OK
       |
       |
       | POST /devices/<dev>/targets/
       |
       |    Create a target
       |
       |    Returns: CREATED
       |
       |
       | GET /devices/<dev>/targets/<id>
       |
       |    Retrieve a target by its id
       |
       |    Returns: OK | NO_CONTENT
       |
       |
       | GET /devices/<dev>/targets/last
       |
       |    Retrieve the last target created
       |
       |    Returns: OK | NO_CONTENT
       |
       |
       | GET /devices/<dev>/targets?from=<timestamp>&to=<timestamp>
       |
       |    Retrieve the list of the targets that where created in between the range provided (timestamp in [ms] since the epoch)
       |
       |    Returns: OK
       |
       |
       | GET /devices/<dev>/targets/summary?status=<status>&consume=<consume>
       |
       |    Retrieve the list of the targets summarized for the device (most recent actor-prop value wins)
       |
       |    The summarized target is generated only using properties that have the given status.
       |    The flag consume tells if the status of the matching properties should be changed from C (created) to X (consumed).
       |
       |    Returns: OK | NO_CONTENT
       |
       |
       | POST /devices/<dev>/actors/<actor>/targets
       |
       |    Create a new target
       |
       |    Returns: CREATED
       |
       |
       | GET /devices/<dev>/actors/<actor>/targets/count?status=<status>
       |
       |    Count the amount of target-properties with the given status
       |
       |    Returns: OK
       |
       |
       | GET /devices/<dev>/actors/<actor>/targets?status=<status>&consume=<consume>
       |
       |    Retrieve the list of the targets for the device-actor (most recent actor-prop value wins)
       |
       |    The list is generated only using properties that have the given status.
       |    The flag consume tells if the status of the matching properties should be changed from C (created) to X (consumed).
       |
       |    Returns: OK
       |
       |
       | GET /devices/<dev>/actors/<actor>/targets/summary?status=<status>&consume=<consume>
       |
       |    Retrieve the summary of the targets for the device-actor (most recent actor-prop value wins)
       |
       |    The summarized target is generated only using properties that have the given status.
       |    The flag consume tells if the status of the matching properties should be changed from C (created) to X (consumed).
       |
       |    Returns: OK | NO_CONTENT
       |
       |
       | GET /devices/<dev>/actors/<actor>/targets/last?status=<status>
       |
       |    Retrieve the last target created for such actor with such status
       |
       |    Returns: OK | NO_CONTENT
       |
       |
    """.stripMargin


  val service = AuthedService[User, IO] {

      // Help

      case GET -> Root / "help" as user =>
        Ok(HelpMsg, ContentTypeTextPlain)

      // Targets & Reports (at device level)

      case a@POST -> Root / "devices" / S(device) / T(table) as user => {
        val x = postDev(a.req, device, table, Time.now)
        Created(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> Root / "devices" / S(device) / T(table) / LongVar(id) as user => {
        val x = getDev(table, id)
        x.flatMap {
          case Some(v) => Ok(v.asJson, ContentTypeAppJson)
          case _ => NoContent()
        }
      }

      case a@GET -> Root / "devices" / S(device) / T(table) / "last" as user => {
        val x = getDevLast(device, table)
        x.flatMap {
          case Some(v) => Ok(v.asJson, ContentTypeAppJson)
          case None => NoContent()
        }
      }

      case a@GET -> Root / "devices" / S(device) / T(table) :? FromP(from) +& ToP(to) as user => {
        val x = getDevAll(device, table, from, to)
        Ok(Stream("[") ++ x.map(_.asJson.noSpaces).intersperse(",") ++ Stream("]"), ContentTypeAppJson)
      }

      case a@GET -> Root / "devices" / S(device) / T(table) / "summary" :? StatusP(status) +& ConsumeP(consume) as user => {
        val x = getDevActorTups(device, None, table, status, consume).map(t => ActorMapU.fromTups(t))
        x.flatMap { m =>
          if (m.isEmpty) {
            NoContent()
          } else {
            Ok(m.asJson, ContentTypeAppJson)
          }
        }
      }

      // Targets & Reports (at device-actor level)

      case a@POST -> Root / "devices" / S(device) / "actors" / S(actor) / T(table) as user => {
        val x = postDevActor(a.req, device, actor, table, Time.now)
        Created(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> Root / "devices" / S(device) / "actors" / S(actor) / T(table) / "count" :? StatusP(status) as user => {
        val x = getDevActorCount(device, actor, table, status)
        Ok(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> Root / "devices" / S(device) / "actors" / S(actor) / T(table) :? StatusP(status) +& ConsumeP(consume) as user => {
        val x = getDevActors(device, actor, table, status, consume)
        Ok(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> Root / "devices" / S(device) / "actors" / S(actor) / T(table) / "summary" :? StatusP(status) +& ConsumeP(consume) as user => {
        val x = getDevActorTups(device, Some(actor), table, status, consume).map(PropsMapU.fromTups)
        x.flatMap { m =>
          if (m.isEmpty) {
            NoContent()
          } else {
            Ok(m.asJson, ContentTypeAppJson)
          }
        }
      }

      case a@GET -> Root / "devices" / S(device) / "actors" / S(actor) / T(table) / "last" :? StatusP(status) as user => {
        val x = getLastDevActorTups(device, actor, table, status).map(PropsMapU.fromTups)
        x.flatMap { m =>
          if (m.isEmpty) {
            NoContent()
          } else {
            Ok(m.asJson, ContentTypeAppJson)
          }
        }
      }

    }

  val serviceWithAuthentication: HttpService[IO] = middleware(service)


  private[v1] def getDev(table: Table, id: Timestamp) = {
    for {
      t <- repository.selectDeviceWhereRequestId(table, id)
      resp <- IO(t.map(DeviceU.fromBom))
    } yield (resp)
  }

  private[v1] def postDev(req: Request[IO], device: DeviceName, table: Table, t: Timestamp) = {
    implicit val x = JsonEncoding.StringDecoder
    for {
      p <- req.decodeJson[ActorMapU]
      id <- repository.insertDevice(table, DeviceU(MetadataU(None, Some(t), device), p).toBom)
      resp <- IO(IdResponse(id))
    } yield (resp)
  }

  private[v1] def postDevActor(req: Request[IO], device: DeviceName, actor: ActorName, table: Table, t: Timestamp) = {
    implicit val x = JsonEncoding.StringDecoder
    for {
      p <- req.decodeJson[PropsMapU]
      id <- repository.insertDevice(table, DeviceU(MetadataU(None, Some(t), device), Map(actor -> p)).toBom)
      resp <- IO(IdResponse(id))
    } yield (resp)
  }

  private[v1] def getDevLast(device: DeviceName, table: Table) = {
    for {
      r <- repository.selectMaxDevice(table, device)
      k <- IO(r.map(DeviceU.fromBom))
    } yield (k)
  }

  private[v1] def getDevAll(device: DeviceName, table: Table, from: Option[Timestamp], to: Option[Timestamp]) = {
    for {
      r <- repository.selectDevicesWhereTimestamp(table, device, from, to).map(DeviceU.fromBom)
    } yield (r)
  }

  private[v1] def getDevActorTups(device: DeviceName, actor: Option[ActorName], table: Table, status: Option[Status], clean: Option[Boolean]) = {
    val actorTups = repository.selectActorTupWhereDeviceActorStatus(table, device, actor, status, clean.exists(identity))
    actorTups.compile.toList
  }

  private[v1] def getLastDevActorTups(device: DeviceName, actor: ActorName, table: Table, status: Option[Status]) = {
    repository.selectMaxActorTupsStatus(table, device, actor, status)
  }

  private[v1] def getDevActors(device: DeviceName, actor: ActorName, table: Table, status: Option[Status], clean: Option[Boolean]) = {
    val actorTups = repository.selectActorTupWhereDeviceActorStatus(table, device, Some(actor), status, clean.exists(identity))
    val t = actorTups.fold(List.empty[ActorTup])(_ :+ _)
    t.map(i => i.groupBy(_.requestId).toList.sortBy(_._1).map(v => PropsMapU.fromTups(v._2)))
  }

  private[v1] def getDevActorCount(device: DeviceName, actor: ActorName, table: Table, statusOp: Option[Status]) = {
    val actorTups = repository.selectActorTupWhereDeviceActorStatus(table, device, Some(actor), statusOp, false)
    actorTups.compile.toList.map(_.size).map(CountResponse(_))
  }

	private[v1] def request(r: Request[IO]): IO[Response[IO]] = serviceWithAuthentication.orNotFound(r)

}

object Service {

  final val ServicePrefix = "/api/v1"

  final val ContentTypeAppJson = `Content-Type`(MediaType.`application/json`)
  final val ContentTypeTextPlain = `Content-Type`(MediaType.`text/plain`)

  case class IdResponse(id: RecordId)
  case class CountResponse(count: Int)

}
