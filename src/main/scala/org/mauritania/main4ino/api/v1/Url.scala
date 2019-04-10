package org.mauritania.main4ino.api.v1

import cats.data.{NonEmptyList, Validated, ValidatedNel}
import org.http4s.{ParseFailure, QueryParamDecoder, QueryParameterValue}
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.mauritania.main4ino.Repository.ReqType
import org.mauritania.main4ino.Repository.ReqType.ReqType
import org.mauritania.main4ino.models.Device.Metadata.Status
import org.mauritania.main4ino.models.Device.Metadata.Status.Status
import org.mauritania.main4ino.models.{EpochSecTimestamp, RequestId}

import scala.util.Try

object Url {

  // Parameters

  implicit val statusDecoder: QueryParamDecoder[Status] = new QueryParamDecoder[Status] {
    override def decode(value: QueryParameterValue): ValidatedNel[ParseFailure, Status] = Validated.Valid(Status(value.value))
  }

  object IdsParam extends OptionalQueryParamDecoderMatcher[Boolean]("ids")
  object StatusParam extends OptionalQueryParamDecoderMatcher[Status]("status")

  object FromParam extends OptionalQueryParamDecoderMatcher[EpochSecTimestamp]("from")
  object ToParam extends OptionalQueryParamDecoderMatcher[EpochSecTimestamp]("to")

  object TimezoneParam extends OptionalQueryParamDecoderMatcher[String]("timezone")

  // Url sections
  object Req { // request type section
    def unapply(str: String): Option[ReqType] = ReqType.resolve(str)
  }

  object Dev { // device section
    final val DeviceRegex = raw"^([a-zA-Z0-9_]{4,20})$$".r
    def unapply(dev: String): Option[String] = DeviceRegex.findFirstIn(dev)
  }

  object Act { // actor section
    final val ActorRegex = raw"^([a-zA-Z0-9_]{4,20})$$".r
    def unapply(dev: String): Option[String] = ActorRegex.findFirstIn(dev)
  }

  object ReqId { // request ID secction
    def unapply(id: String): Option[RequestId] = Try(id.toLong).toOption
  }

}
