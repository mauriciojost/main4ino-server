package org.mauritania.main4ino

import cats.effect.Sync
import org.http4s.EntityBody
import fs2.Stream

object Helper {

  def asEntityBody[F[_]: Sync](content: String): EntityBody[F] = {
    Stream.fromIterator[F, Byte](content.toCharArray.map(_.toByte).toIterator)
  }

}
