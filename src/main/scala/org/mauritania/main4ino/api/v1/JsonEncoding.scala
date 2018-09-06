package org.mauritania.main4ino.api.v1

import io.circe.{Decoder, DecodingFailure}

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


}
