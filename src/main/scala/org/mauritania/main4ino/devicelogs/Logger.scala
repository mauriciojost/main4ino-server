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
import org.mauritania.main4ino.devicelogs.Partitioner.Partition

import scala.concurrent.ExecutionContext

/**
  * Defines a way to handle logs coming from Devices, so that
  * developers can monitor internals of their firmwares without
  * having to pollute the actors' properties.
  *
  * @tparam F
  */
class Logger[F[_] : Sync : ContextShift](config: Config, time: Time[F], ec: ExecutionContext) {

  final private val partitioner = config.partitioner
  final private lazy val blocker = Blocker.liftExecutionContext(ec)
  final private lazy val ChunkSize = 1024
  final private lazy val CreateAndAppend = Seq(StandardOpenOption.CREATE, StandardOpenOption.APPEND)

  private def pathFromDevice(device: DeviceName, partition: Partition): JavaPath =
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
      t <- time.nowUtc
      file = pathFromDevice(device, partitioner.partition(t.toEpochSecond))
      bodyLines = body.through(fs2.text.lines).filter(!_.isEmpty)
      timedBody = bodyLines.map(l => s"${Time.asTimestamp(t)} $l")
      encodedTimedBody = (timedBody.intersperse("\n") ++ Stream.eval(Sync[F].delay("\n"))).through(fs2text.utf8Encode)
      writ <- for {
        preSize <- fileSize(file.toFile)
        written = encodedTimedBody.through(io.file.writeAll[F](file, blocker, CreateAndAppend))
        eithers = written.attempt.compile.toList
        attempts <- eithers.map {
          case Left(e) :: _ => Left(e.getMessage)
          case _ => Right(preSize)
        }
        postSize <- fileSize(file.toFile)
        increase = attempts.map(pre => postSize - pre)
      } yield increase
    } yield writ
  }

  private def isReadableFile(f: File): F[Boolean] = Sync[F].delay(f.canRead && f.isFile)

  private def fileSize(f: File): F[Long] = Sync[F].delay(f.length())

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
    val partitions = (f to t).map(partitioner.partition).toList
    val paths = partitions.map(part => pathFromDevice(device, part))
    val streams: List[F[Stream[F, String]]] = for {
      path <- paths
      stream = for {
        readable <- isReadableFile(path.toFile)
        located: Stream[F, String] = readable match {
          case true => readFile(path)
          case false => Stream.empty
        }
      } yield (located)
    } yield stream
    streams.sequence[F, Stream[F, String]].map(_.reduce(_ ++ _))
  }

  private[devicelogs] def readFile(
    path: JavaPath,
    chunkSize: Int = ChunkSize
  ): Stream[F, String] = {
    val bytes = io.file.readAll[F](path, blocker, chunkSize)
    val bytesLimited = bytes.takeRight(config.maxLengthLogs.value)
    bytesLimited.through(fs2text.utf8Decode).through(fs2text.lines)
  }
}
