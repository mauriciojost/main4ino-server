package org.mauritania.main4ino

import cats.effect.IO
import io.circe.Json
import org.http4s.client.blaze.{BlazeClientBuilder, Http1Client}
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{BasicCredentials, Method, Request, Uri}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, Sequential}
import io.circe.syntax._
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe._
import io.circe.parser._
import org.mauritania.main4ino.api.v1.JsonEncoding
import org.mauritania.main4ino.api.Translator.IdResponse
import org.mauritania.main4ino.models.{DeviceId, RequestId}

class ServerSpec extends FlatSpec with Matchers with BeforeAndAfterAll with HttpClient {

  val OneSecond = 1000

  Sequential
  var appThread: Thread = _
  val UserPass = BasicCredentials(Fixtures.User1.id, Fixtures.User1Pass)

  implicit val statusEncoder = JsonEncoding.StatusEncoder
  implicit val statusDecoder = JsonEncoding.StatusDecoder

  override def beforeAll(): Unit = {
    appThread = launchAsync(Array("src/test/resources/configs/1"))
    Thread.sleep(5 * OneSecond)
  }

  override def afterAll(): Unit = {
    appThread.interrupt()
  }

  "The server" should "start and expose rest the api (v1)" in {
    withHttpClient { httpClient =>
      val help = httpClient.expect[String](s"http://localhost:8080/api/v1/token/${UserPass.token}/help")
      help.unsafeRunSync() should include("https")
    }
  }

  it should "reject unauthorized requests" in {
    withHttpClient { httpClient =>
      assertThrows[UnexpectedStatus] { // forbidden
        httpClient.expect[String]("http://localhost:8080/api/v1/help").unsafeRunSync()
      }
    }
  }

  it should "perform cleanup of old entries regularly" in {
    withHttpClient { httpClient =>

      // T0sec

      // inject dev1
      val dev1ResponseJson = httpClient.expect[String](devPostRequest("dev1", "targets"))
      val id1 = jsonAs[IdResponse](dev1ResponseJson.unsafeRunSync()).id

      // check that dev1 exists
      val dev1t0 = httpClient.expect[String](devGetRequest("dev1", "targets", id1))
      jsonAs[DeviceId](dev1t0.unsafeRunSync()).dbId.id shouldBe id1

      Thread.sleep(4 * OneSecond)

      // T4sec

      // inject dev2
      val dev2ResponseJson = httpClient.expect[String](devPostRequest("dev2", "targets"))
      val id2 = jsonAs[IdResponse](dev2ResponseJson.unsafeRunSync()).id

      // check that dev2 exists
      val dev2t2 = httpClient.expect[String](devGetRequest("dev2", "targets", id2))
      jsonAs[DeviceId](dev2t2.unsafeRunSync()).dbId.id shouldBe (id2)

      // check that dev1 still exists
      val dev1t2 = httpClient.expect[String](devGetRequest("dev1", "targets", id1))
      jsonAs[DeviceId](dev1t2.unsafeRunSync()).dbId.id shouldBe (id1)

      // T4sec

      Thread.sleep(6 * OneSecond)

      // T10sec

      // cleanup every 10s

      Thread.sleep(3 * OneSecond)

      // T13sec

      // check that dev1 does not exist anymore (cleaned up, as 13secs > 10secs + 2 secs retention)
      val dev1t6 = httpClient.expect[String](devGetRequest("dev1", "targets", id1))
      val dev1t6R = dev1t6.unsafeRunSync()
      dev1t6R shouldBe 11

      // check that dev2 exists
      val dev2t6 = httpClient.expect[String](devGetRequest("dev2", "targets", id2))
      jsonAs[DeviceId](dev2t6.unsafeRunSync()).dbId.id shouldBe (id2)

    }

  }

  private def jsonAs[T](json: String)(implicit v: Decoder[T]): T = {
    parse(json).toOption.flatMap(_.as[T].toOption).get
  }

  private def devPostRequest(devName: String, table: String) = {
    Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString(s"http://localhost:8080/api/v1/token/${UserPass.token}/devices/$devName/$table"),
      body = Helper.asEntityBody[IO]("""{"actor1":{"prop1":"val1"}}""")
    )
  }

  private def devGetRequest(devName: String, table: String, id: RequestId) = {
    Request[IO](
      method = Method.GET,
      uri = Uri.unsafeFromString(s"http://localhost:8080/api/v1/token/${UserPass.token}/devices/$devName/$table/$id")
    )
  }

  it should "start and expose the webapp files" in {
    withHttpClient { httpClient =>
      val help = httpClient.expect[String](s"http://localhost:8080/index.html")
      help.unsafeRunSync() should include("</body>")
    }
  }

  it should "start and expose the firmware files" in {
    withHttpClient { httpClient =>
      val help = httpClient.expect[String](s"http://localhost:8080/firmwares/botino/esp8266")
      help.unsafeRunSync() should include("1.0.0")
    }
  }

  it should "fail if started with bad arduments" in {
    // accepts only one argument
    assertThrows[IllegalArgumentException](Server.run(List()).unsafeRunSync())
    assertThrows[IllegalArgumentException](Server.run(List("", "")).unsafeRunSync())
  }

  private def launchAsync(args: Array[String]): Thread = {
    val runnable = new Runnable() {
      override def run() = {
        Server.main(args)
      }
    }
    val thread = new Thread(runnable)
    thread.start()
    thread
  }

}
