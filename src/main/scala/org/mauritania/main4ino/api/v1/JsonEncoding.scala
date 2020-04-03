package org.mauritania.main4ino.api.v1

import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import org.mauritania.main4ino.models.Device.Metadata
import org.mauritania.main4ino.models.Device.Metadata.Status

import scala.util.{Failure, Success}

/**
  * Custom JSON encoders and decoders
  *
  */
object JsonEncoding {

  /**
    * The main4ino-arduino library exchanges JSON messages with main4ino-server.
    * Among other messages, there are messages containing actors' properties. Their types be a string, integer, timing,
    * or any other primitive Value of main4ino.
    * The only assumption made regarding such types is that they can be serialized as string (reason: the will be eventually
    * stored in a database all together in the same column of type TEXT, for simplicity).
    * For such, for example, when a value comes as an integer, it's immediately interpreted at JSON parsing level as a string.
    */
  val StringDecoder: Decoder[String] = { v =>
    val st = Decoder[String].tryDecode(v)
    val in = Decoder[Int].tryDecode(v)
    val bo = Decoder[Boolean].tryDecode(v)
    (st, in, bo) match {
      case (Right(s), _, _) =>
        Right[DecodingFailure, String](s)
      case (_, Right(i), _) =>
        Right[DecodingFailure, String](i.toString)
      case (_, _, Right(b)) =>
        Right[DecodingFailure, String](b.toString)
      case (Left(s), Left(i), Left(b)) =>
        Left[DecodingFailure, String](DecodingFailure.apply(s"Cannot decode '${v}' as none of string/int/bool", Nil))
    }
  }

  val StatusEncoder = new Encoder[Status] {
    override def apply(a: Status): Json = Json.fromString(a.entryName)
  }

  val StatusDecoder: Decoder[Status] = { v =>
    Decoder[String].tryDecode(v).toTry.flatMap(s => Metadata.Status.withNameEither(s).toTry) match {
      case Success(s) => Right[DecodingFailure, Status](s)
      case Failure(f) => Left[DecodingFailure, Status](DecodingFailure(s"Cannot decode $v: ${f.getMessage}", Nil))
    }
  }

}
