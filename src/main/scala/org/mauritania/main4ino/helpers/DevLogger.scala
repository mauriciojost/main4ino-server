package org.mauritania.main4ino.helpers

import java.io.File

import cats.effect.IO
import java.nio.file.{StandardOpenOption, Path => JavaPath}

import org.mauritania.main4ino.api.Attempt
import fs2.{Stream, io, text => fs2text}
import org.mauritania.main4ino.firmware.Store.Firmware
import org.mauritania.main4ino.models.DeviceName

/**
  * Defines a way to handle logs coming from Devices, so that
  * developers can monitor internals of their firmwares without
  * having to pollute the actors' properties.
  * @tparam F
  */
trait DevLogger[F[_]] {
  /**
    * Update logs, provided the device name and the log messages to be appended
    * @param device device name
    * @param body body containing the logs to be appended
    * @return [[Attempt]] telling if it was possible to perform the operation
    */
  def updateLogs(device: DeviceName, body: Stream[F, String]): F[Attempt[Unit]]

  /**
    * Retrieve full logs for the given device
    *
    * Examples:
    * - stream=0123456789
    * - lenght=4 && ignore=0 => 6789
    * - lenght=4 && ignore=1 => 5678
    * - lenght=4 && ignore=2 => 4567
    *
    * @param device device name
    * @param ignore for pagination, amount of bytes to discard from the end
    * @param length for pagination, amount of bytes to take counting backwards from the end
    * @return the [[Attempt]] with the stream containing the chunks of the logs
    */
  def getLogs(device: DeviceName, ignore: Option[Long], length: Option[Long]): F[Attempt[Stream[F, String]]]

}

class DevLoggerIO(basePath: JavaPath, time: Time[IO]) extends DevLogger[IO] {

  final private val ChunkSize = 1024
  final private val CreateAndAppend = Seq(StandardOpenOption.CREATE, StandardOpenOption.APPEND)
  final private val DefaultLengthLogs = 1024L * 10
  final private val DefaultIgnoreLogs = 0L
  final private val MaxLengthLogs = 1024L * 512 // 0.5 MiB

  private def pathFromDevice(device: DeviceName): JavaPath = basePath.resolve(s"$device.log")

  def updateLogs(device: DeviceName, body: Stream[IO, String]): IO[Attempt[Unit]] = {
    val timedBody = Stream.eval[IO, String](time.nowUtc.map("### " + _ + "\n")) ++ body
    val encoded = timedBody.through(fs2text.utf8Encode)
    val written = encoded.to(io.file.writeAll(pathFromDevice(device), CreateAndAppend))
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

  private def isReadableFile(f: File): IO[Boolean] = IO(f.canRead && f.isFile)

  def getLogs(device: DeviceName, ignore: Option[Long], length: Option[Long]): IO[Attempt[Stream[IO, String]]] = {
    val path = pathFromDevice(device)
    val i = ignore.getOrElse(DefaultIgnoreLogs)
    val l = length.map(i => if (i < MaxLengthLogs) i else MaxLengthLogs).getOrElse(DefaultLengthLogs)
    for {
      readable <- isReadableFile(path.toFile)
      located: Attempt[Stream[IO, String]] = readable match {
        case true =>
          Right{
            val bytes = io.file.readAll[IO](pathFromDevice(device), ChunkSize)
            val filteredBytes = bytes.dropRight(i.toInt).takeRight(l)
            val text = filteredBytes.through(fs2text.utf8Decode)
            text
          }
        case false =>
          Left(s"Could not locate/read logs for device: ${device}")
      }
    } yield (located)
  }

}

