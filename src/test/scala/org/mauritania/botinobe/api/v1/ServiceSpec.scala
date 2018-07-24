package org.mauritania.botinobe.api.v1

import cats.effect.IO
import org.http4s._
import org.http4s.implicits._
import org.specs2.matcher.MatchResult

class ServiceSpec extends org.specs2.mutable.Specification {

  "Help request" >> {
    "return 200" >> {
      getApiV1("/help").status must beEqualTo(Status.Ok)
    }
    "return help message" >> {
      getApiV1("/help").as[String].unsafeRunSync() must contain("API HELP")
    }
  }

  private[this] def getApiV1(path: String): Response[IO] = {
    val getHW = Request[IO](Method.GET, Uri.unsafeFromString(path))
    Service.service.orNotFound(getHW).unsafeRunSync()
  }


}
