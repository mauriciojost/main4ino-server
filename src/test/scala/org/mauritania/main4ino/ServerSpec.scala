package org.mauritania.main4ino

import cats.effect.IO
import io.circe.Json
import org.http4s.client.blaze.{BlazeClientBuilder, Http1Client}
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{BasicCredentials, Method, Request, Status, Uri}
import org.scalatest.{BeforeAndAfterAll, Sequential}
import io.circe.syntax._
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe._
import io.circe.parser._
import org.mauritania.main4ino.api.v1.JsonEncoding
import org.mauritania.main4ino.api.Translator.IdResponse
import org.mauritania.main4ino.models.{DeviceId, DeviceName, RequestId}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ServerSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll with HttpClient {

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

      def checkRecord(dev: DeviceName, id: Long, status: Status) = {
        val s = httpClient.status(devGetRequest(dev, "targets", id))
        s.unsafeRunSync() shouldBe status
      }
      def checkExists(dev: DeviceName, id: Long) = checkRecord(dev, id, Status.Ok)
      def checkDoesNotExist(dev: DeviceName, id: Long) = checkRecord(dev, id, Status.NoContent)

      // T0sec

      // inject dev1 (at ~T0sec, cleanup will take place at ~T10sec)
      val dev1ResponseJson = httpClient.expect[String](devPostRequest("dev1", "targets"))
      val idDev1 = jsonAs[IdResponse](dev1ResponseJson.unsafeRunSync()).id

      checkExists("dev1", idDev1) // just created

      Thread.sleep(5 * OneSecond) // T5sec

      // inject dev2 (at ~T5sec, cleanup will take place at ~T15sec)
      val dev2ResponseJson = httpClient.expect[String](devPostRequest("dev2", "targets"))
      val idDev2 = jsonAs[IdResponse](dev2ResponseJson.unsafeRunSync()).id

      checkExists("dev2", idDev2) // just created
      checkExists("dev1", idDev1) // still exists

      Thread.sleep(5 * OneSecond) // T10sec

      // cleanup every 10s

      Thread.sleep(2 * OneSecond) // T12sec

      checkDoesNotExist("dev1", idDev1) // cleaned up
      checkExists("dev2", idDev2) // still there

      Thread.sleep(6 * OneSecond) // T17sec

      checkDoesNotExist("dev2", idDev2) // cleaned up too

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
