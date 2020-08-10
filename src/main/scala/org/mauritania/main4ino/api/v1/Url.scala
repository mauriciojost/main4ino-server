package org.mauritania.main4ino.api.v1

import cats.data.{NonEmptyList, Validated, ValidatedNel}
import org.http4s.{ParseFailure, QueryParamDecoder, QueryParameterValue}
import org.http4s.dsl.impl.{OptionalQueryParamDecoderMatcher, QueryParamDecoderMatcher}
import org.mauritania.main4ino.db.Repository.ReqType
import org.mauritania.main4ino.db.Repository.ReqType.ReqType
import org.mauritania.main4ino.models.Device.Metadata.Status
import org.mauritania.main4ino.models.{EpochSecTimestamp, RequestId}

import scala.util.Try

object Url {

  final val AlphaNumericAndUnderscoreRegex = raw"^([a-zA-Z0-9_]{4,20})$$".r

  def extractSafeStringFrom(s: String): Option[String] =
    AlphaNumericAndUnderscoreRegex.findFirstIn(s)

  // Parameters

  implicit val statusDecoder: QueryParamDecoder[Status] = new QueryParamDecoder[Status] {
    override def decode(value: QueryParameterValue): ValidatedNel[ParseFailure, Status] =
      Status.withNameEither(value.value) match {
        case Right(s) => Validated.Valid(s)
        case Left(f) =>
          Validated.Invalid(
            NonEmptyList(ParseFailure(s"Could not parse: ${value.value}", f.getMessage()), Nil)
          )
      }
  }

  object IdsParam extends OptionalQueryParamDecoderMatcher[Boolean]("ids")
  object StatusParam extends OptionalQueryParamDecoderMatcher[Status]("status")
  object NewStatusParam extends OptionalQueryParamDecoderMatcher[Status]("newstatus")

  object MFromParam extends QueryParamDecoderMatcher[EpochSecTimestamp]("from")
  object MToParam extends QueryParamDecoderMatcher[EpochSecTimestamp]("to")
  object FromParam extends OptionalQueryParamDecoderMatcher[EpochSecTimestamp]("from")
  object ToParam extends OptionalQueryParamDecoderMatcher[EpochSecTimestamp]("to")

  object TimezoneParam extends OptionalQueryParamDecoderMatcher[String]("timezone")

  object VerWishParam extends QueryParamDecoderMatcher[String]("version")

  // Url sections
  object Req { // request type section
    def unapply(str: String): Option[ReqType] = ReqType.resolve(str)
  }

  object Dev { // device section
    def unapply(devId: String): Option[String] = extractSafeStringFrom(devId)
  }

  object Act { // actor section
    def unapply(actorId: String): Option[String] = extractSafeStringFrom(actorId)
  }

  object Proj { // project section
    def unapply(projectId: String): Option[String] = extractSafeStringFrom(projectId)
  }

  object Platf { // platform section (esp8266, esp32, etc.)
    def unapply(platform: String): Option[String] = extractSafeStringFrom(platform)
  }

  object ReqId { // request ID secction
    def unapply(id: String): Option[RequestId] = Try(id.toLong).toOption
  }

}
