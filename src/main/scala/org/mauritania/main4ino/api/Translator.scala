package org.mauritania.main4ino.api

import java.time.{ZoneId, ZonedDateTime}

import cats.effect.Sync
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.dsl.Http4sDsl
import org.mauritania.main4ino.Repository
import org.mauritania.main4ino.Repository.Attempt
import org.mauritania.main4ino.Repository.ReqType.ReqType
import org.mauritania.main4ino.api.Translator.{CountResponse, IdResponse, IdsOnlyResponse, TimeResponse}
import org.mauritania.main4ino.helpers.Time
import org.mauritania.main4ino.models.Device.Metadata.Status.Status
import org.mauritania.main4ino.models._

class Translator[F[_] : Sync](repository: Repository[F], time: Time[F]) extends Http4sDsl[F] {

  def deleteDevice(dev: DeviceName, t: ReqType): F[CountResponse] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      count <- repository.deleteDeviceWhereName(t, dev)
      countResp = CountResponse(count)
      _ <- logger.debug(s"DELETED requests from table $t for device $dev: $countResp")
    } yield (countResp)
  }

  def getDevice(t: ReqType, dev: DeviceName, id: RequestId): F[Attempt[Device]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      device <- repository.selectDeviceWhereRequestId(t, dev, id)
      _ <- logger.debug(s"GET device $id from table $t: $device")
    } yield (device)
  }

  def getDeviceActor(t: ReqType, dev: DeviceName, actor: ActorName, id: RequestId): F[Attempt[ActorProps]] = {
    val x = getDevice(t, dev, id)
    x.map(e => e.flatMap(d => d.actor(actor).toRight(s"No such actor: $actor")))
  }

  def postDevice(dev: F[Device], t: ReqType): F[IdResponse] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      timeUtc <- time.nowUtc
      device <- dev
      id <- repository.insertDevice(t, device, Time.asTimestamp(timeUtc))
      response = IdResponse(id)
      _ <- logger.debug(s"POST device $device into table $t: $response")
    } yield (response)
  }

  def updateDeviceStatus(table: ReqType, device: String, requestId: RequestId, status: Status): F[Attempt[CountResponse]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      updates <- repository.updateDeviceWhereRequestId(table, device, requestId, status)
      count = updates.map(CountResponse)
      _ <- logger.debug(s"Update device $device into table $table: $count")
    } yield (count)
  }

  def postDeviceActor(ap: F[ActorProps], dev: DeviceName, act: ActorName, table: ReqType, id: RequestId): F[Attempt[CountResponse]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      timeUtc <- time.nowUtc
      props <- ap
      inserts <- repository.insertDeviceActor(table, dev, act, id, props, Time.asTimestamp(timeUtc))
      count = inserts.map(CountResponse)
      _ <- logger.debug(s"POST device $dev (actor $act) into table $table: $props / $count")
    } yield (count)
  }

  def getDeviceLast(dev: DeviceName, table: ReqType, status: Option[Status]): F[Option[Device]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      device <- repository.selectMaxDevice(table, dev, status)
      _ <- logger.debug(s"GET last device $dev from table $table: $device")
    } yield (device)
  }

  def getDevicesIds(dev: DeviceName, table: ReqType, from: Option[EpochSecTimestamp], to: Option[EpochSecTimestamp], status: Option[Status]): F[IdsOnlyResponse] = {
    val d = getDevices(dev, table, from, to, status)
    d.map(v => IdsOnlyResponse(v.flatMap(_.metadata.id).toSeq.sorted))
  }

  def getDevices(dev: DeviceName, table: ReqType, from: Option[EpochSecTimestamp], to: Option[EpochSecTimestamp], status: Option[Status]): F[Iterable[Device]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      devices <- repository.selectDevicesWhereTimestampStatus(table, dev, from, to, status)
      _ <- logger.debug(s"GET all devices $dev from table $table from time $from until $to with status $status: $devices")
    } yield (devices)
  }

  def getDevicesSummary(dev: DeviceName, table: ReqType, from: Option[EpochSecTimestamp], to: Option[EpochSecTimestamp], st: Option[Status]): F[Option[Device]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      devices <- repository.selectDevicesWhereTimestampStatus(table, dev, from, to, st)
      summary = devices.reduceOption(Device.merge)
      _ <- logger.debug(s"GET summary all devices $dev from table $table from time $from until $to with status $st: $devices")
    } yield (summary)
  }

  def nowAtTimezone(tz: String): F[TimeResponse] = {
    val tutc = time.nowUtc.map(_.withZoneSameInstant(ZoneId.of(tz)))
    tutc.map(t => TimeResponse(tz, Time.asTimestamp(t), Time.asString(t)))
  }

}

object Translator {

  case class IdResponse(id: RequestId)

  case class CountResponse(count: Int)

  case class IdsOnlyResponse(ids: Seq[RequestId])

  case class TimeResponse(zoneName: String, timestamp: Long, formatted: String) //"zoneName":"Europe\/Paris","timestamp":1547019039,"formatted":"2019-01-09 07:30:39"}

}
