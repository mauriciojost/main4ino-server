package org.mauritania.main4ino.api.v1

import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.mauritania.main4ino.RepositoryIO.Table
import org.mauritania.main4ino.RepositoryIO.Table.Table
import org.mauritania.main4ino.models.{Status, EpochSecTimestamp}

object Url {

  // Parameters

  object StatusParam extends OptionalQueryParamDecoderMatcher[Status]("status")
  object ConsumeParam extends OptionalQueryParamDecoderMatcher[Boolean]("consume")

  object FromParam extends OptionalQueryParamDecoderMatcher[EpochSecTimestamp]("from")
  object ToParam extends OptionalQueryParamDecoderMatcher[EpochSecTimestamp]("to")

  object TimezoneParam extends OptionalQueryParamDecoderMatcher[String]("timezone")

  // Url sections
  object Tbl { // table section
    def unapply(str: String): Option[Table] = Table.resolve(str)
  }

  object Dvc { // device section
    final val DevRegex = raw"^([a-zA-Z0-9_]{4,20})$$".r
    def unapply(dev: String): Option[String] = DevRegex.findFirstIn(dev)
  }


}
