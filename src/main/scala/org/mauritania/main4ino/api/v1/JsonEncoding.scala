package org.mauritania.main4ino.api.v1

import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import org.mauritania.main4ino.models.Device.Metadata
import org.mauritania.main4ino.models.Device.Metadata.Status.Status

object JsonEncoding {

  val StringDecoder: Decoder[String] = { v =>
    val st = Decoder[String].tryDecode(v)
    val in = Decoder[Int].tryDecode(v)
    val bo = Decoder[Boolean].tryDecode(v)
    (st, in, bo) match {
      case (Right(s), _, _) => Right[DecodingFailure, String](s)
      case (_, Right(i), _) => Right[DecodingFailure, String](i.toString)
      case (_, _, Right(b)) => Right[DecodingFailure, String](b.toString)
      case (Left(s), Left(i), Left(b)) => Left[DecodingFailure, String](DecodingFailure.apply(v + s.getMessage() + i.getMessage() + b.getMessage(), Nil))
    }
  }

  val StatusEncoder = new Encoder[Status] {
    override def apply(a: Status): Json = Json.fromString(a.code)
  }

  val StatusDecoder: Decoder[Status] = { v =>
    Decoder[String].tryDecode(v) match {
      case Right(s) => Right[DecodingFailure, Status](Metadata.Status(s))
      case Left(_) => Left[DecodingFailure, Status](DecodingFailure(s"Cannot decode $v", Nil))
    }
  }

}
