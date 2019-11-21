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
    * @param device device name
    * @return the [[Attempt]] with the stream containing the lines of the logs
    */
  def getLogs(device: DeviceName): F[Attempt[Stream[F, String]]]

}

class DevLoggerIO(basePath: JavaPath, time: Time[IO]) extends DevLogger[IO] {

  final val ChunkSize = 1024 * 2
  final val CreateAndAppend = Seq(StandardOpenOption.CREATE, StandardOpenOption.APPEND)

  def pathFromDevice(device: DeviceName): JavaPath = basePath.resolve(s"$device.log")

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

  def getLogs(device: DeviceName): IO[Attempt[Stream[IO, String]]] = {
    val path = pathFromDevice(device)
    for {
      readable <- isReadableFile(path.toFile)
      located: Attempt[Stream[IO, String]] = readable match {
        case true =>
          Right(io.file.readAll[IO](pathFromDevice(device), ChunkSize).through(fs2text.utf8Decode))
        case false =>
          Left(s"Could not locate/read logs for device: ${device}")
      }
    } yield (located)
  }

}

