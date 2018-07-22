package org.mauritania.botinobe.api.v1

import cats.effect.IO
import org.http4s._
import org.http4s.implicits._
import org.specs2.matcher.MatchResult

class ServiceSpec extends org.specs2.mutable.Specification {

  "Help request" >> {
    "return 200" >> {
      uriReturns200()
    }
    "return help message" >> {
      uriReturnsHelp()
    }
  }

  private[this] val retApiV1Help: Response[IO] = {
    val getHW = Request[IO](Method.GET, Uri.uri("/help"))
    Service.service.orNotFound(getHW).unsafeRunSync()
  }

  private[this] def uriReturns200(): MatchResult[Status] =
    retApiV1Help.status must beEqualTo(Status.Ok)

  private[this] def uriReturnsHelp(): MatchResult[String] =
    retApiV1Help.as[String].unsafeRunSync() must contain("API HELP")
}
