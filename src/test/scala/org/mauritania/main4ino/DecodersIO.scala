package org.mauritania.main4ino

import cats.effect.IO
import io.circe.Json
import org.http4s.EntityDecoder
import org.http4s.circe.CirceEntityDecoder._

trait DecodersIO {

  implicit val DecoderIOString = implicitly[EntityDecoder[IO, String]]
  implicit val DecoderIOJson = implicitly[EntityDecoder[IO, Json]]

}
