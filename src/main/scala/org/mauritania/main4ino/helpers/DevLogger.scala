package org.mauritania.main4ino.helpers

import cats.effect.IO
import java.nio.file.{StandardOpenOption, Path => JavaPath}

import org.mauritania.main4ino.api.Attempt
import fs2.{Stream, io, text => fs2text}

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
    * @return [[Attempt]] containing how many lines were appended
    */
  def updateLogs(device: String, body: Stream[F, String]): F[Attempt[Unit]]
}

class DevLoggerIO(basePath: JavaPath) extends DevLogger[IO] {
  final val ConcatChar = '&'

  final val CreateAndAppend = Seq(StandardOpenOption.CREATE, StandardOpenOption.APPEND)

  def updateLogs(device: String, body: Stream[IO, String]): IO[Attempt[Unit]] = {
    val path = basePath.resolve(s"$device.log")

    val encoded = body.through(fs2text.utf8Encode)
    val written = encoded.to(io.file.writeAll(path, CreateAndAppend))
    val eithers = written.attempt.compile.toList
    val attempts = eithers.map {

      // Not clear. This pattern matching is done based on non-documented observed
      // behaviour of io.file.WriteAll whose expected output was a single element
      // containing a Right(()) or a Left(e). Observed: no element if success, one
      // element Left(e) if failure.
      case Nil => Right()
      case Left(e) :: Nil => Left(e.getMessage)
      case _ => Left("Unexpected scenario")
    }

    attempts
  }

}

