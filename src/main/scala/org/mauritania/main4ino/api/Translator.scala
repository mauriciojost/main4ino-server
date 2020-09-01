package org.mauritania.main4ino.api

import java.time.ZoneId

import cats.effect.Sync
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.dsl.Http4sDsl
import org.mauritania.main4ino.db.Repository.ReqType.ReqType
import org.mauritania.main4ino.api.Translator.{
  CountResponse,
  IdResponse,
  IdsOnlyResponse,
  TimeResponse
}
import org.mauritania.main4ino.helpers.Time
import org.mauritania.main4ino.models.Description.VersionJson
import org.mauritania.main4ino.models.Device.Metadata.Status
import org.mauritania.main4ino.models._
import fs2.Stream
import org.mauritania.main4ino.db.Repository
import org.mauritania.main4ino.db.Repository.FromTo
import org.mauritania.main4ino.devicelogs.Logger

class Translator[F[_]: Sync](repository: Repository[F], time: Time[F], devLogger: Logger[F])
    extends Http4sDsl[F] {

  def updateLogs(device: DeviceName, body: Stream[F, String]): F[Attempt[Long]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      d <- devLogger.updateLogs(device, body)
      _ <- logger.debug(s"Appended logs for $device: +$d bytes")
    } yield (d)
  }

  def getLogs(
    device: DeviceName,
    from: EpochSecTimestamp,
    to: EpochSecTimestamp
  ): F[Stream[F, String]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      d <- devLogger.getLogs(device, from, to)
      _ <- logger.debug(s"Retrieved logs for $device")
    } yield (d)
  }

  def getLastDescription(device: DeviceName): F[Attempt[Description]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      d <- repository.getDescription(device)
      _ <- logger.debug(s"Got description for $device")
    } yield (d)
  }

  def updateDescription(device: DeviceName, j: VersionJson): F[Int] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      timeUtc <- time.nowUtc
      inserts <- repository.setDescription(device, j, Time.asTimestamp(timeUtc))
      _ <- logger.debug(s"Set description for ${device}")
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
      _ <- logger.debug(s"GET device $id from table $t")
    } yield (device)
  }

  def postDevice(dev: F[Device], t: ReqType): F[IdResponse] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      timeUtc <- time.nowUtc
      device <- dev
      id <- repository.insertDevice(t, device, Time.asTimestamp(timeUtc))
      response = IdResponse(id)
      _ <- logger.debug(s"POST device $device into table $t")
    } yield (response)
  }

  def getDeviceLast(
    dev: DeviceName,
    table: ReqType,
    status: Option[Status]
  ): F[Option[DeviceId]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      device <- repository.selectMaxDevice(table, dev, status)
      _ <- logger.debug(s"GET last device $dev from table $table with status $status")
    } yield (device)
  }

  def getDevicesIds(
    dev: DeviceName,
    table: ReqType,
    fromTo: FromTo,
    status: Option[Status]
  ): F[IdsOnlyResponse] = {
    val d = getDevices(dev, table, fromTo, status)
    d.map(v => IdsOnlyResponse(v.map(_.dbId.id).toSeq.sorted))
  }

  def getDevices(
    dev: DeviceName,
    table: ReqType,
    fromTo: FromTo,
    status: Option[Status]
  ): F[Iterable[DeviceId]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      devices <- repository.selectDevicesWhereTimestampStatus(table, dev, fromTo, status)
      _ <- logger.debug(
        s"GET all devices $dev from table $table from time ${fromTo.from} until ${fromTo.to} with status $status"
      )
    } yield (devices)
  }

  def getSetDevicesSummary(
    dev: DeviceName,
    table: ReqType,
    st: Option[Status],
    nst: Option[Status]
  ): F[Attempt[Option[Device]]] = {
    val status = st.getOrElse(Status.Closed)
    for {
      logger <- Slf4jLogger.fromClass[F](Translator.getClass)
      devices <- repository.selectDevicesLastWhereStatus(table, dev, status)
      ids = devices.map(d => d.dbId.id).toList
      updates <- nst match {
        case Some(n) => repository.updateDeviceWhereRequestId (table, dev, ids, n)
        case None => Sync[F].pure(List.empty[Attempt[Int]])
      }
      _ <- logger.debug(s"GET summary all devices $dev from table $table with status $status (to $nst, changes: $updates)")
      failures = updates.filter(_.isLeft)
      result = if (failures.isEmpty) {
        Right(Device.merge(devices))
      } else {
        Left(failures.mkString(" ; "))
      }
    } yield (result)
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

  case class CountResponse(count: Long)

  case class IdsOnlyResponse(ids: Seq[RequestId])

  /**
    * Response for a time query
    * @param zoneName an example: Europe\/Paris
    * @param timestamp an example: 1547019039
    * @param formatted an example: 2019-01-09 07:30:39
    */
  case class TimeResponse(zoneName: String, timestamp: Long, formatted: String)

}
