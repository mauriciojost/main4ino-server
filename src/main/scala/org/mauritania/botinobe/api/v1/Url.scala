package org.mauritania.botinobe.api.v1

import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.mauritania.botinobe.Repository.Table
import org.mauritania.botinobe.Repository.Table.Table

object Url {
  // TODO default status query should be ALL! (not "not created")
  object StatusP extends OptionalQueryParamDecoderMatcher[String]("status")
  object ConsumeP extends OptionalQueryParamDecoderMatcher[Boolean]("consume")

  object T {
    def unapply(str: String): Option[Table] = Table.resolve(str)
  }

  object S {
    final val DevRegex = raw"^([a-zA-Z0-9_]{4,20})$$".r
    def unapply(dev: String): Option[String] = DevRegex.findFirstIn(dev)
  }


}
