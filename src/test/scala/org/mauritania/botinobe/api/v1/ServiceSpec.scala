package org.mauritania.botinobe.api.v1

import cats.effect.IO
import org.http4s._
import org.http4s.implicits._
import org.specs2.matcher.MatchResult

class ServiceSpec extends org.specs2.mutable.Specification {

  /*
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
    new Service().orNotFound(getHW).unsafeRunSync()
  }
  */

  /*
  |
  | // create a 2 targets for dev1 and 1 for dev2
  | >  POST /v1/devices/<dev1>/targets/  {"actor1":{"prop1": "val1"}}
  | <    200 {"id": 1}
  | >  POST /v1/devices/<dev1>/targets/  {"actor1":{"prop2": "val2"}}
  | <    200 {"id": 2}
  | >  POST /v1/devices/<dev2>/targets/  {"actor3":{"prop3": "val3"}, "actor4": {"prop4": "val4"}}
  | <    200 {"id": 3}
  | >  POST /v1/devices/<dev2>/targets/  {"actor3":{"prop5": "val5"}}
  | <    200 {"id": 4}
  |
  | // existent targets for dev1
  | >  GET /v1/devices/<dev1>/targets/
  | <    200 {"targets":[1, 2], "count": 2}
  |
  | // get all targets merged dev1 and clean (transactional)
  | >  GET /v1/devices/<dev1>/targets?merge=true&clean=true
  | <    200 {"actor1":{"prop1": "val1", "prop2": "val2"}}
  |
  | // now there are no remaining targets to be consumed
  | >  GET /v1/devices/<dev1>/targets/
  | <    200 {"targets":[], "count": 0}
  |
  | // we can retrieve a specific target
  | >  GET /v1/devices/<dev2>/targets/<3>
  | <    200 {"actor3": {"prop3": "val3"}, "actor4": {"prop4": "val4"}}
  |
  | >  GET /v1/devices/<dev2>/actors/actor3/targets?merge=true&clean=true
  | <    200 {"prop5": "val5"}
  |
  */

}
