package org.mauritania.main4ino.devicelogs

import java.io.File
import java.nio.file.{StandardOpenOption, Path => JavaPath}

import cats.effect.{Blocker, ContextShift, Sync}
import cats.implicits._
import fs2.{Stream, io, text => fs2text}
import org.mauritania.main4ino.api.Attempt
import org.mauritania.main4ino.helpers.Time
import org.mauritania.main4ino.models.{DeviceName, EpochSecTimestamp}
import cats.implicits._

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
  * Defines a way to handle logs coming from Devices, so that
  * developers can monitor internals of their firmwares without
  * having to pollute the actors' properties.
  * @tparam F
  */
class Logger[F[_]: Sync: ContextShift](config: Config, time: Time[F], ec: ExecutionContext) {

  final private val blocker = Blocker.liftExecutionContext(ec)
  final private val ChunkSize = 1024
  final private val CreateAndAppend = Seq(StandardOpenOption.CREATE, StandardOpenOption.APPEND)

  private def pathFromDevice(device: DeviceName): JavaPath = config.logsBasePath.resolve(s"$device.log")

  /**
    * Update logs, provided the device name and the log messages to be appended
    * @param device device name
    * @param body body containing the logs to be appended
    * @return [[Attempt]] telling if it was possible to perform the operation
    */
  def updateLogs(device: DeviceName, body: Stream[F, String]): F[Attempt[Unit]] = {
    val bodyLines = body.through(fs2.text.lines).filter(!_.isEmpty)
    val timedBody = bodyLines.flatMap(l => Stream.eval[F, String](time.nowUtc.map(t => s"${Time.asTimestamp(t)} $l")))
    val encodedTimedBody = (timedBody.intersperse("\n") ++ Stream.eval(Sync[F].delay("\n"))).through(fs2text.utf8Encode)
    val written = encodedTimedBody.through(io.file.writeAll[F](pathFromDevice(device), blocker, CreateAndAppend))
    val eithers = written.attempt.compile.toList
    val attempts = eithers.map {
      // Not clear why this behavior. This pattern matching is done based on non-documented observed
      // behaviour of io.file.WriteAll whose expected output was a single element
      // containing a Right(()) or a Left(e). Observed: no element if success, one
      // element Left(e) if failure.
      case Nil => Right()
      case Left(e) :: Nil => Left(e.getMessage)
      case _ => Left("Unexpected scenario")
    }

    attempts
  }

  private def isReadableFile(f: File): F[Boolean] = Sync[F].delay(f.canRead && f.isFile)

  /**
    * Retrieve full logs for the given device
    *
    * @param device device name
    * @param from for pagination, timestamp from which to retrieve logs
    * @param to for pagination, timestamp until which to retrieve logs
    * @return the [[Attempt]] with the stream containing the chunks of the logs
    */
  def getLogs(device: DeviceName, from: Option[EpochSecTimestamp], to: Option[EpochSecTimestamp]): F[Attempt[Stream[F, Record]]] = {
    val path = pathFromDevice(device)
    for {
      readable <- isReadableFile(path.toFile)
      located: Attempt[Stream[F, Record]] = readable match {
        case true => readFile(device, from, to)
        case false => Left(s"Could not locate/read logs for device: ${device}")
      }
    } yield (located)
  }

  private def readFile(device: DeviceName, from: Option[EpochSecTimestamp], to: Option[EpochSecTimestamp]): Attempt[Stream[F, Record]] = {
    Try {
      val bytes = io.file.readAll[F](pathFromDevice(device), blocker, ChunkSize)
      val bytesLimited = bytes.takeRight(config.maxLengthLogs.value)
      val lines = bytesLimited.through(fs2text.utf8Decode).through(fs2text.lines)
      val records = lines.map(Record.parse).collect { case Some(a) => a }
      val filtered = records.filter(rec => from.forall(f => rec.t >= f) && to.forall(t => rec.t <= t))
      filtered
    }.toEither.swap.map(_.getMessage).swap
  }
}
