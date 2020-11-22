package org.mauritania.main4ino.devicelogs

import java.io.File
import java.nio.file.{StandardOpenOption, Path => JavaPath}

import cats.effect.{Blocker, ContextShift, Sync, Timer}
import cats.implicits._
import fs2.{Stream, io => fs2io, text => fs2text}
import org.mauritania.main4ino.api.Attempt
import org.mauritania.main4ino.helpers.Time
import org.mauritania.main4ino.models.{DeviceName, EpochSecTimestamp}
import cats.implicits._
import io.chrisdavenport.log4cats.{Logger => Log4CatsLogger}
import org.mauritania.main4ino.devicelogs.Partitioner.Partition
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext

/**
  * Defines a way to handle logs coming from Devices, so that
  * developers can monitor internals of their firmwares without
  * having to pollute the actors' properties.
  *
  * @tparam F
  */
class Logger[F[_] : Sync : ContextShift: Timer](config: Config, time: Time[F], e: ExecutionContext) {

  final private[devicelogs] val partitioner : Partitioner.Partitioner = config.partitioner
  final private[devicelogs] lazy val ec: ExecutionContext = e
  final private[devicelogs] lazy val blocker: Blocker = Blocker.liftExecutionContext(ec)
  final private[devicelogs] lazy val ChunkSize = 1024
  final private[devicelogs] lazy val CreateAndAppend = Seq(StandardOpenOption.CREATE, StandardOpenOption.APPEND)

  private[devicelogs] def pathFromDevice(device: DeviceName, partition: Partition): JavaPath =
    config.logsBasePath.resolve(s"$device.$partition.log")

  /**
    * Update logs, provided the device name and the log messages to be appended
    *
    * @param device device name
    * @param body   body containing the logs to be appended
    * @return [[Attempt]] telling if it was possible to perform the operation, and the amount of bytes written
    */
  def updateLogs(device: DeviceName, body: Stream[F, String]): F[Attempt[Long]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](Log4CatsLogger.getClass)
      t <- time.nowUtc
      file = pathFromDevice(device, partitioner.partition(t.toEpochSecond))
      bodyLines = body.through(fs2.text.lines).filter(!_.isEmpty)
      timedBody = bodyLines.map(l => s"${Time.asTimestamp(t)} $l")
      encodedTimedBody = (timedBody.intersperse("\n") ++ Stream.eval(Sync[F].delay("\n"))).through(fs2text.utf8Encode)
      writ <- for {
        preSize <- fileSize(file.toFile)
        written = encodedTimedBody.through(fs2io.file.writeAll[F](file, blocker, CreateAndAppend))
        eithers = written.attempt.compile.toList
        attempts <- eithers.map {
          case Left(er) :: _ => Left(er.getClass.getName + ": " + er.getMessage)
          case _ => Right(preSize)
        }
        postSize <- fileSize(file.toFile)
        increase = attempts.map(pre => postSize - pre)
        _ <- logger.debug(s"Logs updated: $file -> +$increase")
      } yield increase
    } yield writ
  }

  private def isReadableFile(f: File): F[Boolean] = Sync[F].delay(f.canRead && f.isFile)

  private def fileSize(f: File): F[Long] = Sync[F].delay(f.length())

  def filterRecord(f: EpochSecTimestamp, t: EpochSecTimestamp, partitions: Int)(line: String): Boolean = {
    val skipRecordFiltering: Boolean = partitions > config.bypassRecordParsingIfMoreThanPartitions
    if (skipRecordFiltering) {
      true
    } else {
      val spIdx = line.indexOf(' ')
      if (spIdx != -1) {
        val ts = line.substring(0, spIdx).toLong
        ts >= f && ts <= t
      } else {
        false
      }
    }
  }

  /**
    * Retrieve full logs for the given device
    *
    * @param device device name
    * @param f      for pagination, timestamp from which to retrieve logs
    * @param t      for pagination, timestamp until which to retrieve logs
    * @return the [[Attempt]] with the stream containing the chunks of the logs
    */
  def getLogs(
    device: DeviceName,
    f: EpochSecTimestamp,
    t: EpochSecTimestamp
  ): F[Stream[F, String]] = {
    val partitions = partitioner.partitions(f, t)
    val paths = partitions.map(part => pathFromDevice(device, part))
    val filter: String => Boolean = filterRecord(f, t, partitions.length)
    val streams: List[F[Stream[F, String]]] = for {
      path <- paths
      stream = for {
        readable <- isReadableFile(path.toFile)
        located: Stream[F, String] = readable match {
          case true => readFile(path).filterNot(l => l.isEmpty || (l.size == 1 && l.startsWith("\n")))
          case false => Stream.empty
        }
        filtered = located.filter(filter)
      } yield (filtered)
    } yield stream
    streams.sequence[F, Stream[F, String]].map(_.reduce(_ ++ _))
  }

  /**
    * Tail logs (live streaming)
    *
    * @param device device name
    * @return the stream containing the logs for a given device
    */
  def tailLogs(
    device: DeviceName,
    f: EpochSecTimestamp,
  ): F[Stream[F, String]] = {
    val partition = partitioner.partition(f)
    val path = pathFromDevice(device, partition)
    val stream: F[Stream[F, String]] = for {
      readable <- isReadableFile(path.toFile)
      located: Stream[F, String] = readable match {
        case true => tailFile(path)
        case false => Stream.empty
      }
    } yield located
    stream
  }

  private[devicelogs] def readFile(
    path: JavaPath,
    chunkSize: Int = ChunkSize
  ): Stream[F, String] = {
    val bytes = fs2io.file.readAll[F](path, blocker, chunkSize)
    val bytesLimited = bytes.takeRight(config.maxLengthLogs.value)
    bytesLimited.through(fs2text.utf8Decode).through(fs2text.lines)
  }

  private[devicelogs] def tailFile(
    path: JavaPath,
    chunkSize: Int = ChunkSize
  ): Stream[F, String] = {
    val bytes = fs2io.file.tail[F](path, blocker, chunkSize)
    bytes.through(fs2text.utf8Decode).through(fs2text.lines)
  }

}
