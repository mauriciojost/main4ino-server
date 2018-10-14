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
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
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

  val HelpMsg =
    s"""
       | API HELP
       | --- ----
       | GET /help
       |
       |    Display this help
       |
       |    Returns: OK (200)
       |
       |
       | POST /devices/<dev>/targets/
       |
       |    Create a target
       |
       |    Returns: CREATED (201)
       |
       |
       | GET /devices/<dev>/targets/<id>
       |
       |    Retrieve a target by its id
       |
       |    Returns: OK (200) | NO_CONTENT (204)
       |
       |
       | GET /devices/<dev>/targets/last
       |
       |    Retrieve the last target created
       |
       |    Returns: OK (200) | NO_CONTENT (204)
       |
       |
       | GET /devices/<dev>/targets?from=<timestamp>&to=<timestamp>
       |
       |    Retrieve the list of the targets that where created in between the range provided (timestamp in [ms] since the epoch)
       |
       |    Returns: OK (200)
       |
       |
       | GET /devices/<dev>/targets/summary?status=<status>&consume=<consume>
       |
       |    Retrieve the list of the targets summarized for the device (most recent actor-prop value wins)
       |
       |    The summarized target is generated only using properties that have the given status.
       |    The flag consume tells if the status of the matching properties should be changed from C (created) to X (consumed).
       |
       |    Returns: OK (200) | NO_CONTENT (204)
       |
       |
       | GET /devices/<dev>/targets/count?status=<status>
       |
       |    Count the amount of target-properties with the given status for the device
       |
       |    Returns: OK (200)
       |
       |
       | POST /devices/<dev>/actors/<actor>/targets
       |
       |    Create a new target
       |
       |    Returns: CREATED (201)
       |
       |
       | GET /devices/<dev>/actors/<actor>/targets/count?status=<status>
       |
       |    Count the amount of target-properties with the given status
       |
       |    Returns: OK (200)
       |
       |
       | GET /devices/<dev>/actors/<actor>/targets?status=<status>&consume=<consume>
       |
       |    Retrieve the list of the targets for the device-actor (most recent actor-prop value wins)
       |
       |    The list is generated only using properties that have the given status.
       |    The flag consume tells if the status of the matching properties should be changed from C (created) to X (consumed).
       |
       |    Returns: OK (200)
       |
       |
       | GET /devices/<dev>/actors/<actor>/targets/summary?status=<status>&consume=<consume>
       |
       |    Retrieve the summary of the targets for the device-actor (most recent actor-prop value wins)
       |
       |    The summarized target is generated only using properties that have the given status.
       |    The flag consume tells if the status of the matching properties should be changed from C (created) to X (consumed).
       |
       |    Returns: OK (200) | NO_CONTENT (204)
       |
       |
       | GET /devices/<dev>/actors/<actor>/targets/last?status=<status>
       |
       |    Retrieve the last target created for such actor with such status
       |
       |    Returns: OK (200) | NO_CONTENT (204)
       |
       |
    """.stripMargin


  val service = AuthedService[User, IO] {

      // Help

      case GET -> _ / "help" as user =>
        Ok(HelpMsg, ContentTypeTextPlain)

      // Targets & Reports (at device level)

      case a@POST -> _ / "devices" / S(device) / T(table) as user => {
        val x = postDev(a.req, device, table, Time.now)
        Created(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> _ / "devices" / S(device) / T(table) / LongVar(id) as user => {
        val x = getDev(table, id)
        x.flatMap {
          case Some(v) => Ok(v.asJson, ContentTypeAppJson)
          case None => NoContent()
        }
      }

      case a@GET -> _ / "devices" / S(device) / T(table) / "last" as user => {
        val x = getDevLast(device, table)
        x.flatMap {
          case Some(v) => Ok(v.asJson, ContentTypeAppJson)
          case None => NoContent()
        }
      }

      case a@GET -> _ / "devices" / S(device) / T(table) :? FromP(from) +& ToP(to) as user => {
        val x = getDevAll(device, table, from, to)
        Ok(x.map(_.asJson.noSpaces), ContentTypeAppJson)
      }

      case a@GET -> _ / "devices" / S(device) / T(table) / "summary" :? StatusP(status) +& ConsumeP(consume) as user => {
        val x = getDevActorTups(device, None, table, status, consume).map(t => ActorMapU.fromTups(t))
        x.flatMap { m =>
          if (m.isEmpty) {
            NoContent()
          } else {
            Ok(m.asJson, ContentTypeAppJson)
          }
        }
      }

      case a@GET -> _ / "devices" / S(device) / T(table) / "count" :? StatusP(status) as user => {
        val x = getDevActorCount(device, None, table, status)
        Ok(x.map(_.asJson), ContentTypeAppJson)
      }

      // Targets & Reports (at device-actor level)

      case a@POST -> _ / "devices" / S(device) / "actors" / S(actor) / T(table) as user => {
        val x = postDevActor(a.req, device, actor, table, Time.now)
        Created(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> _ / "devices" / S(device) / "actors" / S(actor) / T(table) / "count" :? StatusP(status) as user => {
        val x = getDevActorCount(device, Some(actor), table, status)
        Ok(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> _ / "devices" / S(device) / "actors" / S(actor) / T(table) :? StatusP(status) +& ConsumeP(consume) as user => {
        val x = getDevActors(device, actor, table, status, consume)
        Ok(x.map(_.asJson), ContentTypeAppJson)
      }

      case a@GET -> _ / "devices" / S(device) / "actors" / S(actor) / T(table) / "summary" :? StatusP(status) +& ConsumeP(consume) as user => {
        val x = getDevActorTups(device, Some(actor), table, status, consume).map(PropsMapU.fromTups)
        x.flatMap { m =>
          if (m.isEmpty) {
            NoContent()
          } else {
            Ok(m.asJson, ContentTypeAppJson)
          }
        }
      }

      case a@GET -> _ / "devices" / S(device) / "actors" / S(actor) / T(table) / "last" :? StatusP(status) as user => {
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


  private[v1] def getDev(table: Table, id: RecordId) = {
    for {
      logger <- Slf4jLogger.fromClass[IO](Service.getClass)
      device <- repository.selectDeviceWhereRequestId(table, id)
      deviceU <- IO.pure(device.map(DeviceU.fromBom))
      _ <- logger.debug(s"GET device $id from table $table: $deviceU")
    } yield (deviceU)
  }

  private[v1] def postDev(req: Request[IO], device: DeviceName, table: Table, t: Timestamp) = {
    implicit val x = JsonEncoding.StringDecoder
    for {
      logger <- Slf4jLogger.fromClass[IO](Service.getClass)
      actorMapU <- req.decodeJson[ActorMapU]
      deviceBom = DeviceU(MetadataU(None, Some(t), device), actorMapU).toBom
      id <- repository.insertDevice(table, deviceBom)
      _ <- logger.debug(s"POST device $device into table $table: $deviceBom / $id")
      resp <- IO(IdResponse(id))
    } yield (resp)
  }

  private[v1] def postDevActor(req: Request[IO], device: DeviceName, actor: ActorName, table: Table, t: Timestamp) = {
    implicit val x = JsonEncoding.StringDecoder
    for {
      logger <- Slf4jLogger.fromClass[IO](Service.getClass)
      p <- req.decodeJson[PropsMapU]
      deviceBom = DeviceU(MetadataU(None, Some(t), device), Map(actor -> p)).toBom
      id <- repository.insertDevice(table, deviceBom)
      _ <- logger.debug(s"POST device $device (actor $actor) into table $table: $deviceBom / $id")
      resp <- IO(IdResponse(id))
    } yield (resp)
  }

  private[v1] def getDevLast(device: DeviceName, table: Table) = {
    for {
      logger <- Slf4jLogger.fromClass[IO](Service.getClass)
      r <- repository.selectMaxDevice(table, device)
      deviceBom <- IO.pure(r.map(DeviceU.fromBom))
      _ <- logger.debug(s"GET last device $device from table $table: $deviceBom")
    } yield (deviceBom)
  }

  private[v1] def getDevAll(device: DeviceName, table: Table, from: Option[Timestamp], to: Option[Timestamp]) = {
    for {
      logger <- Slf4jLogger.fromClass[IO](Service.getClass)
      deviceBoms <- repository.selectDevicesWhereTimestamp(table, device, from, to).map(_.map(DeviceU.fromBom))
      _ <- logger.debug(s"GET all devices $device from table $table from $from to $to: $deviceBoms")
    } yield (deviceBoms)
  }

  private[v1] def getDevActorTups(device: DeviceName, actor: Option[ActorName], table: Table, status: Option[Status], clean: Option[Boolean]) = {
    for {
      logger <- Slf4jLogger.fromClass[IO](Service.getClass)
      actorTups <- repository.selectActorTupWhereDeviceActorStatus(table, device, actor, status, clean.exists(identity)).compile.toList
      _ <- logger.debug(s"GET actor tups of device $device actor $actor from table $table with status $status cleaning $clean: $actorTups")
    } yield (actorTups)
  }

  private[v1] def getLastDevActorTups(device: DeviceName, actor: ActorName, table: Table, status: Option[Status]) = {
    repository.selectMaxActorTupsStatus(table, device, actor, status)
  }

  private[v1] def getDevActors(device: DeviceName, actor: ActorName, table: Table, status: Option[Status], clean: Option[Boolean]) = {
    val actorTups = repository.selectActorTupWhereDeviceActorStatus(table, device, Some(actor), status, clean.exists(identity))
    val t = actorTups.fold(List.empty[ActorTup])(_ :+ _)
    t.map(i => i.groupBy(_.requestId).toList.sortBy(_._1).map(v => PropsMapU.fromTups(v._2)))
  }

  private[v1] def getDevActorCount(device: DeviceName, actor: Option[ActorName], table: Table, status: Option[Status]) = {
    for {
      logger <- Slf4jLogger.fromClass[IO](Service.getClass)
      actorTups <- repository.selectActorTupWhereDeviceActorStatus(table, device, actor, status, false).compile.toList
      count <- IO.pure(CountResponse(actorTups.size))
      _ <- logger.debug(s"GET count of device $device actor $actor from table $table with status $status: $count ($actorTups)")
    } yield (count)
  }

	private[v1] def request(r: Request[IO]): IO[Response[IO]] = serviceWithAuthentication.orNotFound(r)

}

object Service {

  final val ContentTypeAppJson = `Content-Type`(MediaType.`application/json`)
  final val ContentTypeTextPlain = `Content-Type`(MediaType.`text/plain`)

  case class IdResponse(id: RecordId)
  case class CountResponse(count: Int)

}
