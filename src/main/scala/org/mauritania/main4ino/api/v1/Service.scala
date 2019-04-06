package org.mauritania.main4ino.api.v1

import java.time.{ZoneId, ZonedDateTime}

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedService, HttpService, MediaType, Request, Response}
import org.mauritania.main4ino.Repository
import org.mauritania.main4ino.RepositoryIO.Table.Table
import org.mauritania.main4ino.api.v1.ActorMapV1.ActorMapV1
import org.mauritania.main4ino.api.v1.DeviceV1.MetadataV1
import org.mauritania.main4ino.api.v1.PropsMapV1.PropsMapV1
import org.mauritania.main4ino.helpers.Time
import org.mauritania.main4ino.models.ActorTup.{Status => AtStatus}
import org.mauritania.main4ino.models.Device.Metadata.{Status => MdStatus}
import org.mauritania.main4ino.models._
import org.mauritania.main4ino.security.Authentication.AccessAttempt
import org.mauritania.main4ino.security.{Authentication, User}

import scala.util.{Failure, Success, Try}

class Service[F[_] : Sync](auth: Authentication[F], repository: Repository[F], time: Time[F]) extends Http4sDsl[F] {

  import Service._
  import Url._

  type ErrMsg = String

  private val HelpMsg =

    s"""
       | API HELP
       | --- ----
       |
       | See: https://github.com/mauriciojost/main4ino-server/blob/master/src/main/scala/org/mauritania/main4ino/api/v1/Service.scala
       |
       | HELP
       | ----
       |
       |
       | GET /help
       |
       |    Display this help.
       |
       |    Returns: OK (200)
       |
       |
       | TIME
       | ----
       |
       | All below queries are useful for time synchronization.
       |
       |
       | GET /time?timezone=<tz>
       |
       |    Return the ISO-local-time formatted time at a given timezone.
       |
       |    Examples of valid timezones: UTC, Europe/Paris, ...
       |    Examples of formatted time: 1970-01-01T00:00:00
       |
       |    Returns: OK (200) | BAD_REQUEST (400)
       |
       |
       | USER
       | ----
       |
       |
       | POST /session (with standard basic auth)
       |
       |    Return the session id from the basic auth provided credentials.
       |
       |    The provided token can be used to authenticate without providing user/password.
       |
       |    Returns: OK (200)
       |
       |
       | GET /user
       |
       |    Return the currently logged in user id.
       |
       |    Returns: OK (200)
       |
       |
       | ADMINISTRATOR
       | -------------
       |
       | All below queries apply to both targets and reports (although in the examples they use targets).
       |
       |
       | DELETE /administrator/devices/<dev>/targets/
       |
       |    Delete all targets for the given device.
       |
       |    To use with extreme care.
       |
       |    Returns: OK (200)
       |
       |
       | DEVICES
       | -------
       |
       | All below queries are applicable to both targets and reports (although in the examples they use targets).
       |
       |
       | POST /devices/<dev>/targets/
       |
       |    Create a target, get the request ID.
       |
       |    Mostly used by the device (mode reports).
       |    If not actor-properties provided the request remains in state open, waiting for properties
       |    to be added. It should be explicitly closed so that it is exposed to devices.
       |
       |    Returns: CREATED (201)
       |
       |
       | PUT /devices/<dev>/targets/<request_id>
       |
       |    Update the target given the device and the request ID.
       |
       |    Returns: OK (200)
       |
       |
       | GET /devices/<dev>/targets/<request_id>
       |
       |    Retrieve a target by its request ID.
       |
       |    Returns: OK (200) | NO_CONTENT (204)
       |
       |
       | GET /devices/<dev>/targets/last
       |
       |    Retrieve the last target created (chronologically).
       |
       |    Returns: OK (200) | NO_CONTENT (204)
       |
       |
       | GET /devices/<dev>/targets?from=<timestamp>&to=<timestamp>
       |
       |    Retrieve the list of the targets that where created in between the time range provided (timestamp in [ms] since the epoch).
       |
       |    Returns: OK (200)
       |
       |
       | GET /devices/<dev>/targets/summary?status=<status>&consume=<consume>
       |
       |    Retrieve the list of the targets summarized for the device (most recent actor-prop value wins).
       |
       |    The summarized target is generated only using properties that have the given status.
       |    The flag consume tells if the status of the matching properties should be changed from C (created) to X (consumed).
       |
       |    Returns: OK (200) | NO_CONTENT (204)
       |
       |
       | GET /devices/<dev>/targets/count?status=<status>
       |
       |    Count the amount of target-properties with the given status for the device.
       |
       |    This is useful to know in advance if is worth to perform a heavier query to retrieve actors properties.
       |
       |    Returns: OK (200)
       |
       |
       | ACTORS
       | ------
       |
       | All below queries apply to both targets and reports (although in the examples they use targets).
       |
       |
       | POST /devices/<dev>/targets/<requestid>/actors/<actor>
       |
       |    Create a new target for a given actor with the provided actor properties.
       |
       |    An existent request can be filled in if the request ID is provided.
       |
       |    Returns: CREATED (201)
       |
       |
       | GET /devices/<dev>/targets/<requestid>/actors/<actor>?status=<status>&consume=<consume>
       |
       |    Retrieve the list of the targets for the device-actor (most recent actor-prop value wins)
       |
       |    The list is generated only using properties that have the given status.
       |    The flag consume tells if the status of the matching properties should be changed from C (created) to X (consumed).
       |
       |    Returns: OK (200)
       |
       |
       | GET /devices/<dev>/targets/actors/<actor>/summary?status=<status>&consume=<consume>
       |
       |    Retrieve the summary of the targets for the device-actor (most recent actor-prop value wins)
       |
       |    The summarized target is generated only using properties that have the given status.
       |    The flag consume tells if the status of the matching properties should be changed from C (created) to X (consumed).
       |
       |    Returns: OK (200) | NO_CONTENT (204)
       |
       |
       | GET /devices/<dev>/targets/actors/<actor>/last?status=<status>
       |
       |    Retrieve the last target created for such actor with such status
       |
       |    Returns: OK (200) | NO_CONTENT (204)
       |
       |
    """.stripMargin

  private[v1] val service = AuthedService[User, F] {

    // Help


    // To be used by developers
    case GET -> _ / "help" as _ =>
      Ok(HelpMsg, ContentTypeTextPlain)

    // Date/Time

    // To be used by devices to get sync in time
    case GET -> _ / "time" :? TimezoneParam(tz) as _ => {
      val attempt = Try(nowAtTimezone(tz.getOrElse("UTC")))
      attempt match {
        case Success(v) => Ok(v.map(_.asJson), ContentTypeTextPlain)
        case Failure(f) => BadRequest()
      }
    }

    // User

    // To be used by web ui to retrieve a session token
    case a@POST -> _ / "session" as user => {
      val session = auth.generateSession(user)
      session.flatMap(s => Ok(s))
    }

    // To be used by web ui to verify login
    case a@GET -> _ / "user" as user => {
      Ok(user.name)
    }


    // Administration

    // To be used by web ui to fully remove records for a given device table
    case a@DELETE -> _ / "administrator" / "devices" / Dvc(device) / Tbl(table) as _ => {
      val x = deleteDev(a.req, device, table)
      Ok(x.map(_.asJson), ContentTypeAppJson)
    }

    // Targets & Reports (at device level)

    // To be used by devices to start a ReqTran (request transaction)
    case a@POST -> _ / "devices" / Dvc(device) / Tbl(table) as _ => {
      val x = postDev(a.req, device, table)
      Created(x.map(_.asJson), ContentTypeAppJson)
    }

    // To be used by devices to commit a request
    case a@PUT -> _ / "devices" / Dvc(device) / Tbl(table) / ReqId(requestId) :? StatusParam(status) as _ => {
      val x = updateRequest(table, device, requestId, status.getOrElse(MdStatus.Closed))
      x.flatMap {
        case Right(v) => Ok(v.asJson, ContentTypeAppJson)
        case Left(v) => NotModified()
      }
    }

    // To be used by ... ? // Useful mainly for testing purposes
    case a@GET -> _ / "devices" / Dvc(device) / Tbl(table) / ReqId(requestId) as _ => {
      val x = getDev(table, device, requestId)
      x.flatMap {
        case Right(v) => Ok(v.asJson, ContentTypeAppJson)
        case Left(v) => NoContent()
      }
    }

    // To be used by devices to retrieve last status upon reboot ??? not used seems
    case a@GET -> _ / "devices" / Dvc(device) / Tbl(table) / "last" as _ => {
      val x = getDevLast(device, table)
      x.flatMap {
        case Some(v) => Ok(v.asJson, ContentTypeAppJson)
        case None => NoContent() // ignore message
      }
    }

    // To be used by web ui to retrieve history of transactions in a given time period
    case a@GET -> _ / "devices" / Dvc(device) / Tbl(table) :? FromParam(from) +& ToParam(to) as _ => {
      val x = getDevAll(device, table, from, to)
      Ok(x.map(_.asJson.noSpaces), ContentTypeAppJson)
    }

    // To be used by web ui to have a summary for a given device
    case a@GET -> _ / "devices" / Dvc(device) / Tbl(table) / "summary" :? StatusParam(status) +& ConsumeParam(consume) as _ => {
      val x = getDevActorTups(device, None, table, status, consume).map(t => ActorMapV1.fromTups(t))
      x.flatMap { m =>
        if (m.isEmpty) {
          NoContent()
        } else {
          Ok(m.asJson, ContentTypeAppJson)
        }
      }
    }

    // To be used by devices to check if it is worth to request existent transactions
    case a@GET -> _ / "devices" / Dvc(device) / Tbl(table) / "count" :? StatusParam(status) as _ => {
      val x = getDevActorCount(device, None, table, status)
      Ok(x.map(_.asJson), ContentTypeAppJson)
    }

    // Targets & Reports (at device-actor level)

    // To be used by tests to push actor reports (actor by actor) creating a new ReqTran
    case a@POST -> _ / "devices" / Dvc(device) / Tbl(table) / "actors" / Dvc(actor) as _ => {
      val x = postDevActor(a.req, device, actor, table, time.nowUtc)
      Created(x.map(_.asJson), ContentTypeAppJson)
    }

    // To be used by devices to push actor reports (actor by actor) to a given existent ReqTran
    case a@POST -> _ / "devices" / Dvc(device) / Tbl(table) / ReqId(rid) / "actors" / Dvc(actor) as _ => {
      val x = postDevActor(a.req, device, actor, table, rid)
      x.flatMap {
        case Right(v) => Created(CountResponse(v).asJson, ContentTypeAppJson)
        case Left(v) => NotModified()
      }
    }

    // To be used for testing mainly
    case a@GET -> _ / "devices" / Dvc(device) / Tbl(table) / "actors" / Dvc(actor) / "count" :? StatusParam(status) as _ => {
      val x = getDevActorCount(device, Some(actor), table, status)
      Ok(x.map(_.asJson), ContentTypeAppJson)
    }

    // To be used by devices to pull actor targets (actor by actor) // used at all ???
    case a@GET -> _ / "devices" / Dvc(device) / Tbl(table) / "actors" / Dvc(actor) :? StatusParam(status) +& ConsumeParam(consume) as _ => {
      val x = getDevActors(device, actor, table, status, consume)
      Ok(x.map(_.asJson), ContentTypeAppJson)
    }

    // To be used by devices to pull actor targets (actor by actor) as a summary
    case a@GET -> _ / "devices" / Dvc(device) / Tbl(table) / "actors" / Dvc(actor) / "summary" :? StatusParam(status) +& ConsumeParam(consume) as _ => {
      val x = getDevActorTups(device, Some(actor), table, status, consume).map(PropsMapV1.fromTups)
      x.flatMap { m =>
        if (m.isEmpty) {
          NoContent()
        } else {
          Ok(m.asJson, ContentTypeAppJson)
        }
      }
    }

    // To be used by devices to see the last status of a given actor (used upon restart)
    case a@GET -> _ / "devices" / Dvc(device) / Tbl(table) / "actors" / Dvc(actor) / "last" :? StatusParam(status) as _ => {
      val x = getLastDevActorTups(device, actor, table, status).map(PropsMapV1.fromTups)
      x.flatMap { m =>
        if (m.isEmpty) {
          NoContent()
        } else {
          Ok(m.asJson, ContentTypeAppJson)
        }
      }
    }
  }

  def deleteDev(req: Request[F], device: DeviceName, table: Table): F[CountResponse] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Service.getClass)
      count <- repository.deleteDeviceWhereName(table, device)
      _ <- logger.debug(s"DELETED $count requests of device $device")
    } yield (CountResponse(count))

  }

  private[v1] def getDev(table: Table, dev: DeviceName, id: RecordId): F[Either[String, DeviceV1]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Service.getClass)
      device <- repository.selectDeviceWhereRequestId(table, dev, id)
      deviceU = device.map(DeviceV1.fromBom)
      _ <- logger.debug(s"GET device $id from table $table: $deviceU")
    } yield (deviceU)
  }

  private[v1] def postDev(req: Request[F], device: DeviceName, table: Table): F[IdResponse] = {
    implicit val x = JsonEncoding.StringDecoder
    for {
      logger <- Slf4jLogger.fromClass[F](Service.getClass)
      t <- time.nowUtc
      ts = Time.asTimestamp(t)
      actorMapU <- req.decodeJson[ActorMapV1]
      status = if (actorMapU.isEmpty) MdStatus.Open else MdStatus.Closed
      deviceBom = DeviceV1(MetadataV1(None, Some(ts), device, status), actorMapU).toBom
      id <- repository.insertDevice(table, deviceBom)
      _ <- logger.debug(s"POST device $device into table $table: $deviceBom / $id")
      resp = IdResponse(id)
    } yield (resp)
  }

  private[v1] def updateRequest(table: Table, device: String, requestId: RecordId, status: MdStatus): F[Either[ErrMsg, Int]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Service.getClass)
      r <- repository.updateDeviceWhereRequestId(table, device, requestId, status)
      _ <- logger.debug(s"Update device $device into table $table: $r")
    } yield (r)
  }

  private[v1] def postDevActor(req: Request[F], device: DeviceName, actor: ActorName, table: Table, requestId: RecordId): F[Either[String, Int]] = {
    implicit val x = JsonEncoding.StringDecoder
    for {
      logger <- Slf4jLogger.fromClass[F](Service.getClass)
      t <- time.nowUtc
      ts = Time.asTimestamp(t)
      p <- req.decodeJson[PropsMapV1]
      r = PropsMapV1.toPropsMap(p, AtStatus.Created)
      s <- repository.insertDeviceActor(table, device, actor, requestId, r, ts)
      _ <- logger.debug(s"POST device $device (actor $actor) into table $table: $r / $s")
    } yield (s)
  }


  private[v1] def postDevActor(req: Request[F], device: DeviceName, actor: ActorName, table: Table, dt: F[ZonedDateTime]): F[IdResponse] = {
    implicit val x = JsonEncoding.StringDecoder
    for {
      logger <- Slf4jLogger.fromClass[F](Service.getClass)
      t <- dt
      ts = Time.asTimestamp(t)
      p <- req.decodeJson[PropsMapV1]
      deviceBom = DeviceV1(MetadataV1(None, Some(ts), device, MdStatus.Closed), Map(actor -> p)).toBom
      id <- repository.insertDevice(table, deviceBom)
      _ <- logger.debug(s"POST device $device (actor $actor) into table $table: $deviceBom / $id")
      resp = IdResponse(id)
    } yield (resp)
  }

  private[v1] def getDevLast(device: DeviceName, table: Table): F[Option[DeviceV1]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Service.getClass)
      r <- repository.selectMaxDevice(table, device)
      deviceBom = r.map(DeviceV1.fromBom)
      _ <- logger.debug(s"GET last device $device from table $table: $deviceBom")
    } yield (deviceBom)
  }

  private[v1] def getDevAll(device: DeviceName, table: Table, from: Option[EpochSecTimestamp], to: Option[EpochSecTimestamp]): F[Iterable[DeviceV1]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Service.getClass)
      deviceBoms <- repository.selectDevicesWhereTimestamp(table, device, from, to).map(_.map(DeviceV1.fromBom))
      _ <- logger.debug(s"GET all devices $device from table $table from time $from until $to: $deviceBoms")
    } yield (deviceBoms)
  }

  private[v1] def getDevActorTups(device: DeviceName, actor: Option[ActorName], table: Table, status: Option[AtStatus], clean: Option[Boolean]): F[Iterable[ActorTup]] = {
    // TODO bugged, can retrieve actor tups whose metadata is open
    for {
      logger <- Slf4jLogger.fromClass[F](Service.getClass)
      actorTups <- repository.selectActorTupWhereDeviceActorStatus(table, device, actor, status, clean.exists(identity)).compile.toList
      _ <- logger.debug(s"GET actor tups of device $device actor $actor from table $table with status $status cleaning $clean: $actorTups")
    } yield (actorTups)
  }

  private[v1] def getLastDevActorTups(device: DeviceName, actor: ActorName, table: Table, status: Option[AtStatus]) = {
    repository.selectMaxActorTupsStatus(table, device, actor, status)
  }

  private[v1] def getDevActors(device: DeviceName, actor: ActorName, table: Table, status: Option[AtStatus], clean: Option[Boolean]): F[Iterable[PropsMapV1]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Service.getClass)
      actorTups <- repository.selectActorTupWhereDeviceActorStatus(table, device, Some(actor), status, clean.exists(identity)).compile.toList
      propsMaps = actorTups.groupBy(_.requestId).toList.sortBy(_._1)
      propsMapsU = propsMaps.map(v => PropsMapV1.fromTups(v._2))
      _ <- logger.debug(s"GET device actors device $device actor $actor from table $table with status $status and clean $clean: $propsMaps ($actorTups)")
    } yield (propsMapsU)
  }


  private[v1] def getDevActorCount(device: DeviceName, actor: Option[ActorName], table: Table, status: Option[AtStatus]): F[CountResponse] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Service.getClass)
      actorTups <- repository.selectActorTupWhereDeviceActorStatus(table, device, actor, status, false).compile.toList
      count = CountResponse(actorTups.size)
      _ <- logger.debug(s"GET count of device $device actor $actor from table $table with status $status: $count ($actorTups)")
    } yield (count)
  }

  private[v1] def logAuthentication(user: AccessAttempt): F[AccessAttempt] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Service.getClass)
      msg = user match {
        case Right(i) => s">>> Authenticated: ${i.name} ${i.email}"
        case Left(m) => s">>> Failed to authenticate: $m"
      }
      _ <- logger.debug(msg)
    } yield (user)
  }

  private[v1] val onFailure: AuthedService[String, F] = Kleisli(req => OptionT.liftF(Forbidden(req.authInfo)))
  private[v1] val customAuthMiddleware: AuthMiddleware[F, User] =
    AuthMiddleware(Kleisli(auth.authenticateAndCheckAccessFromRequest) andThen Kleisli(logAuthentication), onFailure)
  val serviceWithAuthentication: HttpService[F] = customAuthMiddleware(service)

  private[v1] def request(r: Request[F]): F[Response[F]] = serviceWithAuthentication.orNotFound(r)

  private def nowAtTimezone(tz: String): F[TimeResponse] = {
    val tutc = time.nowUtc.map(_.withZoneSameInstant(ZoneId.of(tz)))
    tutc.map(t => TimeResponse(tz, Time.asTimestamp(t), Time.asString(t)))
  }

}

object Service {

  final val ContentTypeAppJson = `Content-Type`(MediaType.`application/json`)
  final val ContentTypeTextPlain = `Content-Type`(MediaType.`text/plain`)

  case class IdResponse(id: RecordId)

  case class CountResponse(count: Int)

  case class TimeResponse(zoneName: String, timestamp: Long, formatted: String) //"zoneName":"Europe\/Paris","timestamp":1547019039,"formatted":"2019-01-09 07:30:39"}

}

