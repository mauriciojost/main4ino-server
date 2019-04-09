package org.mauritania.main4ino.api

import java.time.{ZoneId, ZonedDateTime}

import cats.effect.Sync
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.dsl.Http4sDsl
import org.mauritania.main4ino.Repository
import org.mauritania.main4ino.Repository.Table.Table
import org.mauritania.main4ino.api.Translator.{CountResponse, IdResponse, TimeResponse}
import org.mauritania.main4ino.helpers.Time
import org.mauritania.main4ino.models.Device.Metadata
import org.mauritania.main4ino.models.Device.Metadata.{Status => MdStatus}
import org.mauritania.main4ino.models._

class Translator[F[_] : Sync](repository: Repository[F], time: Time[F]) extends Http4sDsl[F] {

  def deleteDev(device: DeviceName, table: Table): F[CountResponse] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      count <- repository.deleteDeviceWhereName(table, device)
      _ <- logger.debug(s"DELETED $count requests of device $device")
    } yield (CountResponse(count))
  }

  def getDev(table: Table, dev: DeviceName, id: RecordId): F[Either[String, Device]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      device <- repository.selectDeviceWhereRequestId(table, dev, id)
      _ <- logger.debug(s"GET device $id from table $table: $device")
    } yield (device)
  }

  def postDev(de: F[Device], table: Table): F[IdResponse] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      d <- de
      id <- repository.insertDevice(table, d)
      _ <- logger.debug(s"POST device into table $table: $d / $id")
      resp = IdResponse(id)
    } yield (resp)
  }

  def updateRequest(table: Table, device: String, requestId: RecordId, status: MdStatus): F[Either[ErrMsg, Int]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      r <- repository.updateDeviceWhereRequestId(table, device, requestId, status)
      _ <- logger.debug(s"Update device $device into table $table: $r")
    } yield (r)
  }

  def postDevActor(pm: F[PropsMap], device: DeviceName, actor: ActorName, table: Table, requestId: RecordId): F[Either[String, Int]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      t <- time.nowUtc
      ts = Time.asTimestamp(t)
      p <- pm
      s <- repository.insertDeviceActor(table, device, actor, requestId, p, ts)
      _ <- logger.debug(s"POST device $device (actor $actor) into table $table: $p / $s")
    } yield (s)
  }

  def postDevActor(pm: F[PropsMap], device: DeviceName, actor: ActorName, table: Table, dt: F[ZonedDateTime]): F[IdResponse] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      t <- dt
      ts = Time.asTimestamp(t)
      p <- pm
      deviceBom = Device(Metadata(None, Some(ts), device, MdStatus.Closed), Map(actor -> p))
      id <- repository.insertDevice(table, deviceBom)
      _ <- logger.debug(s"POST device $device (actor $actor) into table $table: $deviceBom / $id")
      resp = IdResponse(id)
    } yield (resp)
  }

  def getDevLast(device: DeviceName, table: Table, status: Option[MdStatus]): F[Option[Device]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      r <- repository.selectMaxDevice(table, device, status)
      deviceBom = r
      _ <- logger.debug(s"GET last device $device from table $table: $deviceBom")
    } yield (deviceBom)
  }

  def getDevAll(device: DeviceName, table: Table, from: Option[EpochSecTimestamp], to: Option[EpochSecTimestamp], st: Option[MdStatus]): F[Iterable[Device]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      deviceBoms <- repository.selectDevicesWhereTimestampStatus(table, device, from, to, st)
      _ <- logger.debug(s"GET all devices $device from table $table from time $from until $to with status $st: $deviceBoms")
    } yield (deviceBoms)
  }

  def getDevAllSummary(device: DeviceName, table: Table, from: Option[EpochSecTimestamp], to: Option[EpochSecTimestamp], st: Option[MdStatus]): F[Option[Device]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      deviceBoms <- repository.selectDevicesWhereTimestampStatus(table, device, from, to, st)
      summary = deviceBoms.reduceOption(Device.merge)
      _ <- logger.debug(s"GET summary all devices $device from table $table from time $from until $to with status $st: $deviceBoms")
    } yield (summary)
  }

  /*
  def getDevActorTups(device: DeviceName, actor: Option[ActorName], table: Table, status: Option[AtStatus], clean: Option[Boolean]): F[Iterable[ActorTup]] = {
    // TODO bugged, can retrieve actor tups whose metadata is open
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      actorTups <- repository.selectActorTupWhereDeviceActorStatus(table, device, actor, status, clean.exists(identity)).compile.toList
      _ <- logger.debug(s"GET actor tups of device $device actor $actor from table $table with status $status cleaning $clean: $actorTups")
    } yield (actorTups)
  }
  */

  /*
  Can be replaced retrieving the last device and checking its actor (firmware will be such that all state is pushed within the same request)
  def getLastDevActorTups(device: DeviceName, actor: ActorName, table: Table, status: Option[AtStatus]) = {
    repository.selectMaxActorTupsStatus(table, device, actor, status)
  }
  */

  /*
  def getDevActors(device: DeviceName, actor: ActorName, table: Table, status: Option[AtStatus], clean: Option[Boolean]): F[Iterable[PropsMap]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      actorTups <- repository.selectActorTupWhereDeviceActorStatus(table, device, Some(actor), status, clean.exists(identity)).compile.toList
      propsMaps = actorTups.groupBy(_.requestId).toList.sortBy(_._1)
      p = propsMaps.map(v => PropsMap.fromTups(v))
      _ <- logger.debug(s"GET device actors device $device actor $actor from table $table with status $status and clean $clean: $propsMaps ($actorTups)")
    } yield (p)
  }
  */

  /*
  // retrieve better the count of requests with status closed for the given device
  def getDevActorCount(device: DeviceName, actor: Option[ActorName], table: Table, status: Option[AtStatus]): F[CountResponse] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      actorTups <- repository.selectActorTupWhereDeviceActorStatus(table, device, actor, status, false).compile.toList
      count = CountResponse(actorTups.size)
      _ <- logger.debug(s"GET count of device $device actor $actor from table $table with status $status: $count ($actorTups)")
    } yield (count)
  }
  */

  def nowAtTimezone(tz: String): F[TimeResponse] = {
    val tutc = time.nowUtc.map(_.withZoneSameInstant(ZoneId.of(tz)))
    tutc.map(t => TimeResponse(tz, Time.asTimestamp(t), Time.asString(t)))
  }

}

object Translator {

  case class IdResponse(id: RecordId)

  case class CountResponse(count: Int)

  case class IdsOnlyResponse(ids: Seq[RecordId])

  case class TimeResponse(zoneName: String, timestamp: Long, formatted: String) //"zoneName":"Europe\/Paris","timestamp":1547019039,"formatted":"2019-01-09 07:30:39"}

}
