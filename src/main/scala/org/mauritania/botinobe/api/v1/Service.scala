package org.mauritania.botinobe.api.v1

import cats.data.Validated
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
import io.circe._
import org.mauritania.botinobe.Repository.Table
import org.mauritania.botinobe.Repository.Table.Table
import org.mauritania.botinobe.api.v1.ActorMapU.ActorMapU
import org.mauritania.botinobe.api.v1.DeviceU.MetadataU
import org.mauritania.botinobe.api.v1.PropsMapU.PropsMapU
import org.mauritania.botinobe.helpers.Time
import io.circe._
import io.circe.generic.semiauto._
import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.encoding.DerivedObjectEncoder

// Guidelines for REST:
// - https://blog.octo.com/wp-content/uploads/2014/10/RESTful-API-design-OCTO-Quick-Reference-Card-2.2.pdf

class Service(repository: Repository) extends Http4sDsl[IO] {

  final val MinDeviceNameLength = 4
  final val ContentTypeAppJson = `Content-Type`(MediaType.`application/json`)
  final val ContentTypeTextPlain = `Content-Type`(MediaType.`text/plain`)

  object CreatedMr extends OptionalQueryParamDecoderMatcher[Boolean]("created")
  object MergeMr extends OptionalQueryParamDecoderMatcher[Boolean]("merge")
  object CleanMr extends OptionalQueryParamDecoderMatcher[Boolean]("clean")

  object T {
    def unapply(str: String): Option[Table] = Table.resolve(str)
  }

  object S {
    final val DevRegex = raw"([a-zA-Z0-9_]{4,20})".r
    def unapply(dev: String): Option[String] = DevRegex.findFirstIn(dev)
  }

  val decoderString: Decoder[String] = { v =>
    val s = Decoder[String].tryDecode(v).map(identity)
    val i = Decoder[Int].tryDecode(v).map(_.toString)
    val b = Decoder[Boolean].tryDecode(v).map(_.toString)
    Right[DecodingFailure, String](s.getOrElse(i.getOrElse(b.getOrElse(""))))
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
       | GET  /devices/<device_name>/actors/<actor_name>/reports  params: created[boolean], count[boolean], clean[boolean], merge[boolean]
       | ...
       |
    """.stripMargin

  val service =
    HttpService[IO] {

      /////////////////
      // Help

      case GET -> Root / "help" =>
        Ok(HelpMsg, ContentTypeTextPlain)

      // Targets & Reports (at device level)

      case a@POST -> Root / "devices" / S(device) / T(table) => {
        val x = postDev(a, device, table, Time.now)
        Created(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> Root / "devices" / S(device) / T(table) / LongVar(id) => {
        val x = getDev(table, id)
        Ok(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> Root / "devices" / S(device) / T(table) / "last" => {
        val x = getDevLast(device, table)
        Ok(x.map(_.asJson), ContentTypeAppJson)
      }

      // Targets & Reports (at device-actor level)

      case a@POST -> Root / "devices" / S(device) / "actors" / S(actor) / T(table) => {
        val x = postDevActor(a, device, actor, table, Time.now)
        Created(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> Root / "devices" / S(device) / "actors" / S(actor) / T(table) / "count" :? CreatedMr(created) => {
        val x = getDevActorCount(device, actor, table, created)
        Ok(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> Root / "devices" / S(device) / "actors" / S(actor) / T(table) :? CreatedMr(created) +& CleanMr(clean) => {
        val x = getDevActors(device, actor, table, created, clean)
        Ok(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> Root / "devices" / S(device) / "actors" / S(actor) / T(table)/ "summary" :? CreatedMr(created) +& CleanMr(clean) => {
        val x = getDevActorsSummary(device, actor, table, created, clean)
        Ok(x.map(_.asJson), ContentTypeAppJson)
      }

    }

  private[v1] def getDev(table: Table, id: Timestamp) = {
    for {
      t <- repository.selectDeviceWhereRequestId(table, id)
      resp <- IO(DeviceU.fromBom(t))
    } yield (resp)
  }

  private[v1] def postDev(req: Request[IO], device: DeviceName, table: Table, t: Timestamp) = {
    implicit val x = decoderString
    for {
      p <- req.decodeJson[ActorMapU]
      id <- repository.insertDevice(table, DeviceU(MetadataU(None, Some(t), device), p).toBom)
      resp <- IO(IdResponse(id))
    } yield (resp)
  }

  private[v1] def postDevActor(req: Request[IO], device: DeviceName, actor: ActorName, table: Table, t: Timestamp) = {
    implicit val x = decoderString
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

  private[v1] def getDevActorsSummary(device: DeviceName, actor: ActorName, table: Table, created: Option[Boolean], clean: Option[Boolean]) = {
    val selectStatus = if (created.exists(identity)) Status.Created else Status.Consumed
    val actorTups = repository.selectActorTupWhereDeviceActorStatus(table, device, Some(actor), selectStatus, clean.exists(identity))
    val t = actorTups.fold(List.empty[ActorTup])(_ :+ _)
    t.map(i => PropsMapU.fromTups(i))
  }

  private[v1] def getDevActors(device: DeviceName, actor: ActorName, table: Table, created: Option[Boolean], clean: Option[Boolean]) = {
    val selectStatus = if (created.exists(identity)) Status.Created else Status.Consumed
    val actorTups = repository.selectActorTupWhereDeviceActorStatus(table, device, Some(actor), selectStatus, clean.exists(identity))
    val t = actorTups.fold(List.empty[ActorTup])(_ :+ _)
    t.map(i => i.groupBy(_.requestId).map(j => PropsMapU.fromTups(j._2)).toList)
  }

  private[v1] def getDevActorCount(device: DeviceName, actor: ActorName, table: Table, createdOp: Option[Boolean]) = {
    val selectStatus = if (createdOp.exists(identity)) Status.Created else Status.Consumed
    val actorTups = repository.selectActorTupWhereDeviceActorStatus(table, device, Some(actor), selectStatus, true)
    actorTups.map(_ => 1).reduce(_ + _).lastOr(0).map(CountResponse(_))
  }

	private[v1] def request(r: Request[IO]): IO[Response[IO]] = service.orNotFound(r)

}

object Service {

  case class IdResponse(id: RecordId)
  case class CountResponse(count: Int)
  case class DevicesResponse(response: Seq[DeviceU])

}
