package org.mauritania.main4ino.api.v1

import java.time.{ZoneId, ZonedDateTime}

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Decoder.Result
import io.circe.Json.JString
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedService, HttpService, MediaType, Request, Response}
import org.mauritania.main4ino.Repository
import org.mauritania.main4ino.Repository.Attempt
import org.mauritania.main4ino.Repository.ReqType.ReqType
import org.mauritania.main4ino.api.Translator
import org.mauritania.main4ino.api.Translator.CountResponse
import org.mauritania.main4ino.helpers.Time
import org.mauritania.main4ino.models.Description.VersionJson
import org.mauritania.main4ino.models.Device.Metadata
import org.mauritania.main4ino.models.Device.Metadata.Status
import org.mauritania.main4ino.models._
import org.mauritania.main4ino.security.Auther.{AccessAttempt, UserSession}
import org.mauritania.main4ino.security.{Auther, User}

import scala.util.{Failure, Success, Try}

class Service[F[_] : Sync](auth: Auther[F], tr: Translator[F], time: Time[F]) extends Http4sDsl[F] {

  import Service._
  import Url._

  type ErrMsg = String

  private val HelpMsg = "See: https://github.com/mauriciojost/main4ino-server/blob/master/src/main/scala/org/mauritania/main4ino/api/v1/Service.scala"

  implicit val jsonStringDecoder = JsonEncoding.StringDecoder
  implicit val jsonStatusDecoder = JsonEncoding.StatusDecoder
  implicit val jsonStatusEncoder = JsonEncoding.StatusEncoder

  private[v1] val service = AuthedService[User, F] {

    /**
      * GET /help
      *
      * Display this help.
      *
      * To be used by developers to use the REST API.
      *
      * Returns: OK (200)
      */
    case GET -> _ / "help" as _ =>
      Ok(HelpMsg, ContentTypeTextPlain)

    // Date/Time

    /**
      * GET /time?timezone=<tz>
      *
      * Example: GET /time?timezone=UTC
      *
      * Return the ISO-local-time formatted time at a given timezone.
      *
      * Examples of valid timezones: UTC, Europe/Paris, ...
      * Examples of formatted time: 1970-01-01T00:00:00
      * To be used by devices to get time synchronization.
      *
      * Returns: OK (200) | BAD_REQUEST (400)
      */
    case GET -> _ / "time" :? TimezoneParam(tz) as _ => {
      val attempt: Try[F[Translator.TimeResponse]] = Try(tr.nowAtTimezone(tz.getOrElse("UTC")))
      attempt match {
        case Success(v) => Ok(v.map(_.asJson), ContentTypeTextPlain)
        case Failure(f) => BadRequest()
      }
    }

    // User

    /**
      * POST /session (with standard basic auth)
      *
      * Return the session id from the 'basic auth' provided credentials.
      * The provided token can be used to authenticate without providing user/password.
      * To be used by web ui to retrieve a session token that can be provided via cookies.
      *
      * Returns: OK (200)
      */
    case a@POST -> _ / "session" as user => {
      val session: F[UserSession] = auth.generateSession(user)
      session.flatMap(s => Ok(s, ContentTypeTextPlain))
    }

    /**
      * GET /user
      *
      * Return the currently logged in user id.
      * To be used by web ui to verify user logged in.
      *
      * Returns: OK (200)
      */
    case a@GET -> _ / "user" as user => {
      Ok(user.name, ContentTypeTextPlain)
    }


    // Administration

    /**
      * DELETE /administrator/devices/<dev>/targets
      *
      * Example: DELETE /administrator/devices/dev1/targets
      *
      * Delete all targets for the given device.
      * To use with extreme care.
      * To be used by administrator on a web ui to fully remove records for a given device table.
      *
      * Returns: OK (200)
      */
    case a@DELETE -> _ / "administrator" / "devices" / Dev(device) / Req(table) as _ => {
      val x: F[Translator.CountResponse] = tr.deleteDevice(device, table)
      Ok(x.map(_.asJson), ContentTypeAppJson)
    }
    /**
      * POST /devices/<dev>/descriptions
      *
      * Example: POST /devices/dev1/descriptions
      *
      * Update the device description given the device ID.
      *
      * Device describes actors and properties with PUT, web ui uses them via GET.
      *
      * Returns: CREATED (201)
      */
    case a@POST -> _ / "devices" / Dev(device) / "descriptions" as _ => {
      val d = a.req.decodeJson[VersionJson]
      val r: F[CountResponse] = d.flatMap(i => tr.updateDescription(device, i)).map(CountResponse(_))
      r.flatMap { v =>
        Created(v.asJson, ContentTypeAppJson)
      }
    }

    /**
      * GET /devices/<dev>/descriptions
      *
      * Example: GET /devices/dev1/descriptions
      *
      * Retrieve a device description given the device ID.
      *
      * Device describes actors and properties with PUT, web ui GET.
      *
      * Returns: OK (200) | NO_CONTENT (204)
      */
    case a@GET -> _ / "devices" / Dev(device) / "descriptions" as _ => {
      val x: F[Attempt[Description]] = tr.getLastDescription(device)
      x.flatMap {
        case Right(v) => Ok(v.asJson, ContentTypeAppJson)
        case Left(_) => NoContent()
      }
    }


    // Targets & Reports (at device level)

    /**
      * POST /devices/<dev>/targets
      *
      * Example: POST /devices/dev1/targets
      *
      * Create a target, get the request ID.
      *
      * Mostly used by the device (mode reports) to start a request transaction.
      * There are 2 main scenarios:
      * - no actor-property provided: the request remains in state open, waiting for properties
      *   to be added in a second step. It should be explicitly closed so that it is exposed to devices.
      * - at least one actor-property is provided: the request is automatically closed, and exposed to devices.
      *
      * Returns: CREATED (201)
      */
    case a@POST -> _ / "devices" / Dev(device) / Req(table) as _ => {
      val am = a.req.decodeJson[DeviceProps]
      val d = for {
        a <- am
        de = Device(device, a)
      } yield (de)
      val x: F[Translator.IdResponse] = tr.postDevice(d, table)
      Created(x.map(_.asJson), ContentTypeAppJson)
    }

    /**
      * GET /devices/<dev>/targets?from=<timestamp>&to=<timestamp>&status=<status>&ids=<idsonly>
      *
      * Example: GET /devices/dev1/targets?from=1535790000&to=1535790099&status=C&ids=true
      *
      * Retrieve the list of the targets that where created in between the time range
      * provided (timestamp in [sec] since the epoch) and with the given status.
      * It is possible to retrieve only the record ids via boolean <idsonly>.
      *
      * To be used by web ui to retrieve history of transactions in a given time period with a given status.
      * To be used by the devices to retrieve pull ids (ids of targets/reports with a given status).
      *
      * Returns: OK (200)
      */
    case a@GET -> _ / "devices" / Dev(device) / Req(table) :? FromParam(from) +& ToParam(to) +& StatusParam(st) +& IdsParam(ids) as _ => {
      val idsOnly = ids.exists(identity)
      if (idsOnly) {
        val x: F[Translator.IdsOnlyResponse] = tr.getDevicesIds(device, table, from, to, st)
        Ok(x.map(_.asJson), ContentTypeAppJson)
      } else {
        val x: F[Iterable[DeviceId]] = tr.getDevices(device, table, from, to, st)
        Ok(x.map(_.asJson), ContentTypeAppJson)
      }
    }

    /**
      * GET /devices/<dev>/targets/summary?from=1535790000&to=1535790099&status=C
      *
      * Example: GET /devices/dev1/targets/summary?from=<timestamp>&to=<timestamp>&status=<status>
      *
      * Retrieve the list of the targets summarized for the device (most recent actor-prop value wins).
      *
      * The summarized target is generated only using properties that have the given status.
      * The flag consume tells if the status of the matching properties should be changed from C (created) to X (consumed).
      *
      * To be used by web ui to retrieve summary of transactions in a given time period with a given status.
      *
      * Returns: OK (200) | NO_CONTENT (204)
      *
      */
    case a@GET -> _ / "devices" / Dev(device) / Req(table) / "summary" :? FromParam(from) +& ToParam(to) +& StatusParam(st) as _ => {
      val x: F[Option[Device]] = tr.getDevicesSummary(device, table, from, to, st)
      x.flatMap {
        case Some(v) => Ok(v.actors.asJson, ContentTypeAppJson)
        case None => NoContent()
      }
    }

    /**
      *
      * PUT /devices/<dev>/targets/<request_id>
      *
      * Example: PUT /devices/dev1/targets/1000
      *
      * Update the target given the device and the request ID.
      *
      * To be used by devices to commit a request that was filled with actor-properties.
      *
      * Returns: OK (200) | NOT_MODIFIED (304)
      */
    case a@PUT -> _ / "devices" / Dev(device) / Req(table) / ReqId(requestId) :? StatusParam(st) as _ => {
      val x: F[Attempt[Translator.CountResponse]] = tr.updateDeviceStatus(table, device, requestId, st.getOrElse(Status.Closed))
      x.flatMap {
        case Right(v) => Ok(v.asJson, ContentTypeAppJson)
        case Left(_) => NotModified()
      }
    }

    // TODO DEPRECATED, to be removed (and adapt arduino part consequently)
    case a@POST -> _ / "devices" / Dev(device) / Req(table) / ReqId(requestId) :? StatusParam(st) as _ => {
      val x: F[Attempt[Translator.CountResponse]] = tr.updateDeviceStatus(table, device, requestId, st.getOrElse(Status.Closed))
      x.flatMap {
        case Right(v) => Ok(v.asJson, ContentTypeAppJson)
        case Left(_) => NotModified()
      }
    }

    /**
      * GET /devices/<dev>/targets/<request_id>
      *
      * Example: GET /devices/dev1/targets/1000
      *
      * Retrieve a target by its request ID.
      *
      * Useful mainly for testing purposes.
      *
      * Returns: OK (200) | NO_CONTENT (204)
      */
    case a@GET -> _ / "devices" / Dev(device) / Req(table) / ReqId(requestId) as _ => {
      val x: F[Attempt[DeviceId]] = tr.getDevice(table, device, requestId)
      x.flatMap {
        case Right(v) => Ok(v.asJson, ContentTypeAppJson)
        case Left(v) => NoContent()
      }
    }

    /**
      * GET /devices/<dev>/targets/last?status=<status>
      *
      * Example: GET /devices/dev1/targets/last?status=C
      *
      * Retrieve the last target created with the given status (chronologically).
      * Used for testing.
      *
      * Returns: OK (200) | NO_CONTENT (204)
      */
    case a@GET -> _ / "devices" / Dev(device) / Req(table) / "last" :? StatusParam(status) as _ => {
      val x: F[Option[DeviceId]] = tr.getDeviceLast(device, table, status)
      x.flatMap {
        case Some(v) => Ok(v.asJson, ContentTypeAppJson)
        case None => NoContent() // ignore message
      }
    }

    // Targets & Reports (at device-actor level)

    /**
      * POST /devices/<dev>/targets/actors/<actor>
      *
      * Example: POST /devices/dev1/targets/actors/clock
      *
      * Create a new target for a given actor with the provided actor properties.
      *
      * To be used by tests to push actor reports (actor by actor) creating a new transaction.
      *
      * Returns: CREATED (201)
      */
    case a@POST -> _ / "devices" / Dev(device) / Req(table) / "actors" / Act(actor) as _ => {
      val actorProps = a.req.decodeJson[ActorProps]
      val dev = for {
        p <- actorProps
        dev = Device(device, Map(actor -> p))
      } yield (dev)
      val x: F[Translator.IdResponse] = tr.postDevice(dev, table)
      Created(x.map(_.asJson), ContentTypeAppJson)
    }

    /**
      * POST /devices/<dev>/targets/<requestid>/actors/<actor>
      *
      * Example: POST /devices/dev1/targets/1000/actors/clock
      *
      * Create a new target for a given actor with the provided actor properties.
      *
      * An existent request can be filled in if the request ID is provided.
      * To be used by devices to push actor reports (actor by actor) to a given existent transaction.
      *
      * Returns: CREATED (201) | NOT_MODIFIED (304)
      */
    case a@POST -> _ / "devices" / Dev(device) / Req(table) / ReqId(rid) / "actors" / Act(actor) as _ => {
      val pm = a.req.decodeJson[ActorProps]
      val x: F[Attempt[Translator.CountResponse]] = tr.postDeviceActor(pm, device, actor, table, rid)
      x.flatMap {
        case Right(v) => Created(v.asJson, ContentTypeAppJson)
        case Left(v) => NotModified()
      }
    }

    /**
      * GET /devices/<dev>/targets/actors/<actor>/last?status=<status>
      *
      * Example: GET /devices/dev1/targets/actors/clock/last?status=C
      *
      * Retrieve the last target created for such actor with such status.
      *
      * To be used by devices to see the last status of a given actor (status = C, used upon restart).
      *
      * Returns: OK (200) | NO_CONTENT (204)
      */
    case a@GET -> _ / "devices" / Dev(device) / Req(table) / "actors" / Act(actor) / "last" :? StatusParam(status) as _ => {
      val x: F[Option[DeviceId]] = tr.getDeviceLast(device, table, status)
      x.flatMap { m =>
        val v = m.flatMap(_.device.actor(actor))
        v match {
          case Some(x) => Ok(x.asJson, ContentTypeAppJson)
          case None => NoContent()
        }
      }
    }

    /**
      * GET /devices/<dev>/targets/<requestid>/actors/<actor>
      *
      * Example: GET /devices/dev1/targets/1000/actors/clock
      *
      * Retrieve the corresponding properties in the given request for the given actor.
      *
      * To be used by devices to pull actor targets (actor by actor).
      *
      * Returns: OK (200) | NO_CONTENT (204)
      */
    case a@GET -> _ / "devices" / Dev(device) / Req(table) / ReqId(rid) / "actors" / Act(actor) as _ => {
      val x: F[Attempt[ActorProps]] = tr.getDeviceActor(table, device, actor, rid)
      x.flatMap {
        case Right(d) => Ok(d.asJson, ContentTypeAppJson)
        case Left(_) => NoContent()
      }
    }
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

}

object Service {

  final val ContentTypeAppJson = `Content-Type`(MediaType.`application/json`)
  final val ContentTypeTextPlain = `Content-Type`(MediaType.`text/plain`)

}

