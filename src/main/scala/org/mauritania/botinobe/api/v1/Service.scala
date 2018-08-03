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
import org.mauritania.botinobe.Repository.Table
import org.mauritania.botinobe.Repository.Table.Table
import org.mauritania.botinobe.api.v1.ActorMapU.ActorMapU
import org.mauritania.botinobe.api.v1.DeviceU.MetadataU
import org.mauritania.botinobe.api.v1.PropsMapU.PropsMapU
import org.mauritania.botinobe.helpers.Time
import io.circe._
import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}
import org.http4s.{AuthedService, Cookie, Request, Response, headers}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.server.blaze.BlazeBuilder

import org.http4s.util.string._
import org.http4s.headers.Authorization
import org.reactormonk.{CryptoBits, PrivateKey}
import java.time._

import cats._
import cats.effect._
import cats.implicits._
import cats.data._
import cats.syntax.EitherObjectOps
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server._
import cats.syntax.either._
import org.http4s.util.string._
import org.http4s.headers.Authorization
// import org.http4s.headers.Authorization

import cats.syntax.either._

// Guidelines for REST:
// - https://blog.octo.com/wp-content/uploads/2014/10/RESTful-API-design-OCTO-Quick-Reference-Card-2.2.pdf

class Service(repository: Repository) extends Http4sDsl[IO] {

  final val MinDeviceNameLength = 4
  final val ContentTypeAppJson = `Content-Type`(MediaType.`application/json`)
  final val ContentTypeTextPlain = `Content-Type`(MediaType.`text/plain`)

  // TODO default status query should be ALL! (not "not created")
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

  case class User(id: Long, name: String)

  val key = PrivateKey(scala.io.Codec.toUTF8(scala.util.Random.alphanumeric.take(20).mkString("")))

  val crypto = CryptoBits(key)

  val clock = Clock.systemUTC

  def verifyLogin(request: Request[IO]): IO[Either[String,User]] = ??? // gotta figure out how to do the form

  val logIn: Kleisli[IO, Request[IO], Response[IO]] = Kleisli({ request =>
    verifyLogin(request: Request[IO]).flatMap(_ match {
      case Left(error) =>
        Forbidden(error)
      case Right(user) => {
        val message = crypto.signToken(user.id.toString, clock.millis.toString)
        Ok("Logged in!").map(_.addCookie(Cookie("authcookie", message)))
      }
    })
  })

  def retrieveUser: Kleisli[IO, Long, User] = Kleisli(id => IO(???))

  val authUser: Kleisli[IO, Request[IO], Either[String, User]] = Kleisli({ request =>
    val message = for {
      header <- request.headers.get(Authorization).toRight("Couldn't find an Authorization header")
      token <- crypto.validateSignedToken(header.value).toRight("Cookie invalid")
      msg <- new cats.syntax.EitherObjectOps(Either).catchOnly[NumberFormatException](token.toLong).leftMap(_.toString)
    } yield(msg)
    message.traverse(retrieveUser.run)
  })

  val onFailure: AuthedService[String, IO] = Kleisli(req => OptionT.liftF(Forbidden(req.authInfo)))

  val middleware = AuthMiddleware(authUser, onFailure)

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


  val serviceSec: HttpService[IO] = middleware(service)

  val service =
    AuthedService[User, IO] {

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

      // Targets & Reports (at device-actor level)

      case a@POST -> Root / "devices" / S(device) / "actors" / S(actor) / T(table) as user => {
        val x = postDevActor(a.req, device, actor, table, Time.now)
        Created(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> Root / "devices" / S(device) / "actors" / S(actor) / T(table) / "count" :? CreatedMr(created) as user => {
        val x = getDevActorCount(device, actor, table, created)
        Ok(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> Root / "devices" / S(device) / "actors" / S(actor) / T(table) :? CreatedMr(created) +& CleanMr(clean) as user => {
        val x = getDevActors(device, actor, table, created, clean)
        Ok(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> Root / "devices" / S(device) / "actors" / S(actor) / T(table)/ "summary" :? CreatedMr(created) +& CleanMr(clean) as user => {
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

  private[v1] def getDevAll(device: DeviceName, table: Table) = {
    for {
      r <- repository.selectDevices(table, device).map(DeviceU.fromBom)
    } yield (r)
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

	private[v1] def request(r: Request[IO]): IO[Response[IO]] = serviceSec.orNotFound(r)

}

object Service {

  case class IdResponse(id: RecordId)
  case class CountResponse(count: Int)
  case class DevicesResponse(response: Seq[DeviceU])

}
