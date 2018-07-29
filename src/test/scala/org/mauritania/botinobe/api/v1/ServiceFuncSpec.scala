package org.mauritania.botinobe.api.v1

import cats.effect.IO
import org.mauritania.botinobe.{DbSuite, Repository}
import org.http4s.{EntityBody, HttpService, MediaType, Method, Request, Response, Uri}
import fs2.Stream

class ServiceFuncSpec extends DbSuite {

  "The repository" should "create and read a target/report" in {
    implicit val s = new Service(new Repository(transactor))

    get("/help") should include("API HELP")

    post("/devices/dev1/targets", """{"clock": {"h":"7"}}""") shouldBe("""{"id":1}""")
    post("/devices/dev1/targets", """{"clock": {"m":"0"}}""") shouldBe("""{"id":2}""")
    post("/devices/dev1/targets", """{"clock": {"s":"1"}}""") shouldBe("""{"id":3}""")
    post("/devices/dev1/targets", """{"body": {"mv0":"Zz."}}""") shouldBe("""{"id":4}""")
    post("/devices/dev1/targets", """{"body": {"mv0":"Zz1"}}""") shouldBe("""{"id":5}""")
    post("/devices/dev2/targets", """{"body": {"mv0":"Zz."}}""") shouldBe("""{"id":6}""") // on dev2
    post("/devices/dev1/targets", """{"clock": {"s":"9"}}""") shouldBe("""{"id":7}""")

    get("/devices/dev1/targets/last") should include("""{"clock":{"s":"9"}}""")

    // TODO keep adding more
    //get("/devices/dev1/targets?clean=false&merge=false&count=false")
    //get("/devices/dev1/targets?clean=false&merge=false&count=true") shouldBe("""{"count":6}""")
    //get("/devices/dev1/targets?clean=false&merge=true&count=false") should contain("""{"body":{"mv0":"Zz1"},"clock":{"h":"7","s":"9","m":"0"}}""")
    //get("/devices/dev1/actors/clock/targets?clean=false&merge=false&count=false") shouldBe("""{"body"{"mv0":"7"},"clock"{"h":"0","m":"7","s":"7"}}""")

  }

  private[this] def get(path: String)(implicit service: Service): String = {
    val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(path))
    service.request(request).unsafeRunSync().as[String].unsafeRunSync()
  }

  private[this] def post(path: String, body: String)(implicit service: Service): String = {
    val request = Request[IO](method = Method.POST, uri = Uri.unsafeFromString(path), body = asEntityBody(body))
    service.request(request).unsafeRunSync().as[String].unsafeRunSync()
  }

  private [this] def asEntityBody(content: String): EntityBody[IO] = {
    Stream.fromIterator[IO, Byte](content.toCharArray.map(_.toByte).toIterator)
  }

}
