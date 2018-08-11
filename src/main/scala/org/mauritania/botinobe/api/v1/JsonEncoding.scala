package org.mauritania.botinobe.api.v1

import io.circe.{Decoder, DecodingFailure}

object JsonEncoding {

  val StringDecoder: Decoder[String] = { v =>
    val s = Decoder[String].tryDecode(v).map(identity)
    val i = Decoder[Int].tryDecode(v).map(_.toString)
    val b = Decoder[Boolean].tryDecode(v).map(_.toString)
    Right[DecodingFailure, String](s.getOrElse(i.getOrElse(b.getOrElse("")))) // TODO: to be done correctly (no failure error accumulated)
  }


}
