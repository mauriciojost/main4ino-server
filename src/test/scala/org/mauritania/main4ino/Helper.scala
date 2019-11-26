package org.mauritania.main4ino

import java.util.concurrent.Executors

import cats.effect.Sync
import org.http4s.EntityBody
import fs2.Stream

import scala.concurrent.ExecutionContext

object Helper {

  def asEntityBody[F[_]: Sync](content: String): EntityBody[F] = {
    Stream.fromIterator[F](content.toCharArray.map(_.toByte).toIterator)
  }

  def global: ExecutionContext = ExecutionContext.fromExecutor(Executors.newScheduledThreadPool(20))

}
