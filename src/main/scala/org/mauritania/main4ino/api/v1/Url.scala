package org.mauritania.main4ino.api.v1

import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.mauritania.main4ino.Repository.Table
import org.mauritania.main4ino.Repository.Table.Table
import org.mauritania.main4ino.models.{Status, Timestamp}

object Url {
  // TODO default status query should be ALL! (not "not created")
  object StatusP extends OptionalQueryParamDecoderMatcher[Status]("status")
  object ConsumeP extends OptionalQueryParamDecoderMatcher[Boolean]("consume")

  object FromP extends OptionalQueryParamDecoderMatcher[Timestamp]("from")
  object ToP extends OptionalQueryParamDecoderMatcher[Timestamp]("to")

  object T {
    def unapply(str: String): Option[Table] = Table.resolve(str)
  }

  object S {
    final val DevRegex = raw"^([a-zA-Z0-9_]{4,20})$$".r
    def unapply(dev: String): Option[String] = DevRegex.findFirstIn(dev)
  }


}
