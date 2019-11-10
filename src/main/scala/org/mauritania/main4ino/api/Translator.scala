package org.mauritania.main4ino.api

import java.nio.file.Paths
import java.time.ZoneId

import cats.effect.Sync
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.dsl.Http4sDsl
import org.mauritania.main4ino.Repository
import org.mauritania.main4ino.Repository.ReqType.ReqType
import org.mauritania.main4ino.api.Translator.{CountResponse, IdResponse, IdsOnlyResponse, TimeResponse}
import org.mauritania.main4ino.helpers.{DevLogger, Time}
import org.mauritania.main4ino.models.Description.VersionJson
import org.mauritania.main4ino.models.Device.Metadata.Status.Status
import org.mauritania.main4ino.models._
import fs2.Stream
import org.mauritania.main4ino.firmware.Store

class Translator[F[_]: Sync](repository: Repository[F], time: Time[F], devLogger: DevLogger[F], firmware: Store[F]) extends Http4sDsl[F] {

  def getFirmware(project: ProjectName, firmwareId: FirmwareId): F[Attempt[Stream[F, Byte]]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      y <- firmware.getFirmware(project, firmwareId)
      _ <- logger.debug(s"Lookup firmware $project/$firmwareId")
    } yield (y)
  }

  def updateLogs(device: DeviceName, body: Stream[F, String]): F[Attempt[Unit]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      d <- devLogger.updateLogs(device, body)
      _ <- logger.debug(s"Appended logs for $device: $d")
    } yield (d)
  }

  def getLastDescription(device: DeviceName): F[Attempt[Description]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      d <- repository.getDescription(device)
      _ <- logger.debug(s"Got description for $device: $d")
    } yield (d)
  }

  def updateDescription(device: DeviceName, j: VersionJson): F[Int] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      timeUtc <- time.nowUtc
      inserts <- repository.setDescription(device, j, Time.asTimestamp(timeUtc))
      _ <- logger.debug(s"Set description for ${device}: $j")
    } yield (inserts)
  }


  def deleteDevice(dev: DeviceName, t: ReqType): F[CountResponse] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      count <- repository.deleteDeviceWhereName(t, dev)
      countResp = CountResponse(count)
      _ <- logger.debug(s"DELETED requests from table $t for device $dev: $countResp")
    } yield (countResp)
  }

  def getDevice(t: ReqType, dev: DeviceName, id: RequestId): F[Attempt[DeviceId]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      device <- repository.selectDeviceWhereRequestId(t, dev, id)
      _ <- logger.debug(s"GET device $id from table $t: $device")
    } yield (device)
  }

  def getDeviceActor(t: ReqType, dev: DeviceName, actor: ActorName, id: RequestId): F[Attempt[ActorProps]] = {
    val x = getDevice(t, dev, id)
    x.map(e => e.flatMap(d => d.device.actor(actor).toRight(s"No such actor: $actor")))
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
      _ <- logger.debug(s"Update device $device into table $table id $requestId to $status: count $count")
    } yield (count)
  }

  def postDeviceActor(ap: F[ActorProps], dev: DeviceName, act: ActorName, table: ReqType, id: RequestId): F[Attempt[CountResponse]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      timeUtc <- time.nowUtc
      props <- ap
      inserts <- repository.insertDeviceActor(table, dev, act, id, props, Time.asTimestamp(timeUtc))
      count = inserts.map(CountResponse)
      _ <- logger.debug(s"POST device $dev (actor $act) into table $table id $id: $props / $count")
    } yield (count)
  }

  def getDeviceLast(dev: DeviceName, table: ReqType, status: Option[Status]): F[Option[DeviceId]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      device <- repository.selectMaxDevice(table, dev, status)
      _ <- logger.debug(s"GET last device $dev from table $table with status $status: $device")
    } yield (device)
  }

  def getDeviceActorLast(dev: DeviceName, act: ActorName, table: ReqType, status: Option[Status]): F[Option[DeviceId]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      device <- repository.selectMaxDeviceActor(table, dev, act, status)
      _ <- logger.debug(s"GET last device $dev actor $act from table $table with status $status: $device")
    } yield (device)
  }

  def getDevicesIds(dev: DeviceName, table: ReqType, from: Option[EpochSecTimestamp], to: Option[EpochSecTimestamp], status: Option[Status]): F[IdsOnlyResponse] = {
    val d = getDevices(dev, table, from, to, status)
    d.map(v => IdsOnlyResponse(v.map(_.dbId.id).toSeq.sorted))
  }

  def getDevices(dev: DeviceName, table: ReqType, from: Option[EpochSecTimestamp], to: Option[EpochSecTimestamp], status: Option[Status]): F[Iterable[DeviceId]] = {
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
      summary = Device.merge(devices)
      _ <- logger.debug(s"GET summary all devices $dev from table $table from time $from until $to with status $st: $devices")
    } yield (summary)
  }

  def nowAtTimezone(tz: String): F[TimeResponse] = {
    for {
      t <- time.nowUtc
      ts = t.withZoneSameInstant(ZoneId.of(tz))
    } yield (TimeResponse(tz, Time.asTimestamp(ts), Time.asString(ts)))
  }

}

object Translator {

  case class IdResponse(id: RequestId)

  case class CountResponse(count: Int)

  case class IdsOnlyResponse(ids: Seq[RequestId])

  case class TimeResponse(zoneName: String, timestamp: Long, formatted: String) //"zoneName":"Europe\/Paris","timestamp":1547019039,"formatted":"2019-01-09 07:30:39"}

}
