package org.mauritania.main4ino.api.v1

import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import org.mauritania.main4ino.models.Device.Metadata
import org.mauritania.main4ino.models.Device.Metadata.Status.Status

/**
  * Custom JSON encoders and decoders
  *
  */
object JsonEncoding {

  /**
    * The main4ino-arduino library exchanges JSON messages with main4ino-server.
    * The schemas of those messages are generated from strongly typed classes. Among other messages, there are
    * messages containing values of actor's properties. The types of such values can be a string, an integer, a timing, any primitive
    * Value of main4ino. The only assumption made regarding such types is that they are serializable as a string (reason, the need of storing
    * it into a database). For such, for example, when a value comes as an integer, it's immediately interpreted at JSON
    * parsing level as a string.
    */
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
