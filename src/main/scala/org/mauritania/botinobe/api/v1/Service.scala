package org.mauritania.botinobe.api.v1

import io.circe.syntax._
import org.http4s.circe._
import io.circe.generic.auto._
import org.http4s.{HttpService, MediaType}
import org.mauritania.botinobe.Repository
import org.mauritania.botinobe.models._
import org.http4s.headers.`Content-Type`
import org.mauritania.botinobe.Repository.Table.Table
import org.mauritania.botinobe.api.v1.ActorMapU.ActorMapU
import org.mauritania.botinobe.api.v1.DeviceU.MetadataU
import org.mauritania.botinobe.api.v1.PropsMapU.PropsMapU
import org.mauritania.botinobe.helpers.Time
import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import fs2.Stream
import org.http4s.{AuthedService, Request, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.mauritania.botinobe.models.Device.Metadata
import org.mauritania.botinobe.security.{Authentication, User}

class Service(auth: Authentication, repository: Repository) extends Http4sDsl[IO] {

  import Service._
  import Url._

  val onFailure: AuthedService[String, IO] = Kleisli(req => OptionT.liftF(Forbidden(req.authInfo)))
  val middleware = AuthMiddleware(auth.authUser, onFailure)

  val HelpMsg = // TODO: complete me
    s"""
       | API HELP
       | --- ----
       | // About help
       | GET /help
       |
    """.stripMargin


  val service = AuthedService[User, IO] {

      /////////////////
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
        Ok(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> Root / "devices" / S(device) / T(table) / "last" as user => {
        val x = getDevLast(device, table)
        Ok(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> Root / "devices" / S(device) / T(table) as user => {
        val x = getDevAll(device, table)
        Ok(Stream("[") ++ x.map(_.asJson.noSpaces).intersperse(",") ++ Stream("]"), ContentTypeAppJson)
      }

      case a@GET -> Root / "devices" / S(device) / T(table) / "summary" :? StatusP(status) +& ConsumeP(clean) as user => {
        val x = getDevActorTups(device, None, table, status, clean)
        Ok(x.map(t => DeviceU.fromBom(Device.fromActorTups(Metadata(None, None, device), t))).map(_.asJson), ContentTypeAppJson)
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

      case a@GET -> Root / "devices" / S(device) / "actors" / S(actor) / T(table) :? StatusP(status) +& ConsumeP(clean) as user => {
        val x = getDevActors(device, actor, table, status, clean)
        Ok(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> Root / "devices" / S(device) / "actors" / S(actor) / T(table)/ "summary" :? StatusP(status) +& ConsumeP(clean) as user => {
        val x = getDevActorTups(device, Some(actor), table, status, clean)
        Ok(x.map(PropsMapU.fromTups).map(_.asJson), ContentTypeAppJson)
      }

    }

  val serviceWithAuthentication: HttpService[IO] = middleware(service)


  private[v1] def getDev(table: Table, id: Timestamp) = {
    for {
      t <- repository.selectDeviceWhereRequestId(table, id)
      resp <- IO(DeviceU.fromBom(t))
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
      k <- IO(DeviceU.fromBom(r))
    } yield (k)
  }

  private[v1] def getDevAll(device: DeviceName, table: Table) = {
    for {
      r <- repository.selectDevices(table, device).map(DeviceU.fromBom)
    } yield (r)
  }

  private[v1] def getDevActorTups(device: DeviceName, actor: Option[ActorName], table: Table, status: Option[Status], clean: Option[Boolean]) = {
    val actorTups = repository.selectActorTupWhereDeviceActorStatus(table, device, actor, status, clean.exists(identity))
    actorTups.fold(List.empty[ActorTup])(_ :+ _)
  }

  private[v1] def getDevActors(device: DeviceName, actor: ActorName, table: Table, status: Option[Status], clean: Option[Boolean]) = {
    val actorTups = repository.selectActorTupWhereDeviceActorStatus(table, device, Some(actor), status, clean.exists(identity))
    val t = actorTups.fold(List.empty[ActorTup])(_ :+ _)
    t.map(i => i.groupBy(_.requestId).toList.sortBy(_._1).map(v => PropsMapU.fromTups(v._2)))
  }

  private[v1] def getDevActorCount(device: DeviceName, actor: ActorName, table: Table, statusOp: Option[Status]) = {
    val actorTups = repository.selectActorTupWhereDeviceActorStatus(table, device, Some(actor), statusOp, false)
    actorTups.map(_ => 1).reduce(_ + _).lastOr(0).map(CountResponse(_))
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
