package org.mauritania.main4ino

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import org.http4s.client.UnexpectedStatus
import org.http4s.{BasicCredentials, Method, Request, Status, Uri}
import org.scalatest.{Assertion, BeforeAndAfterAll}
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
import pureconfig.error.ConfigReaderException

class ServerSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll with HttpClient {

  val InitializationTimeMs = 5000
  val TimeUnit = 500 // unit of time in ms for this test suite

  lazy val appThread: Thread = launchAsync()
  val UserPass = BasicCredentials(Fixtures.User1.id, Fixtures.User1Pass)

  implicit val statusEncoder = JsonEncoding.StatusEncoder
  implicit val statusDecoder = JsonEncoding.StatusDecoder

  override def beforeAll(): Unit = {
    System.setProperty("config-dir", "src/test/resources/configs/1")
    ConfigFactory.invalidateCaches() // force reload of java properties
    appThread
    Thread.sleep(InitializationTimeMs) // give time to the thread to initialize
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

      def checkRecord(dev: DeviceName, id: Long, status: Status): Assertion = {
        val s = httpClient.status(devGetRequest(dev, "targets", id))
        s.unsafeRunSync() shouldBe status
      }
      def checkExists(dev: DeviceName, id: Long): Assertion = checkRecord(dev, id, Status.Ok)
      def checkDoesNotExist(dev: DeviceName, id: Long): Assertion = checkRecord(dev, id, Status.NoContent)

      // T0timeunit

      // inject dev1 (at ~T0timeunit, cleanup will take place at ~T10timeunit)
      val dev1ResponseJson = httpClient.expect[String](devPostRequest("dev1", "targets"))
      val idDev1 = jsonAs[IdResponse](dev1ResponseJson.unsafeRunSync()).id

      checkExists("dev1", idDev1) // just created

      Thread.sleep(5 * TimeUnit) // T5timeunit

      // inject dev2 (at ~T5timeunit, cleanup will take place at ~T15timeunit)
      val dev2ResponseJson = httpClient.expect[String](devPostRequest("dev2", "targets"))
      val idDev2 = jsonAs[IdResponse](dev2ResponseJson.unsafeRunSync()).id

      checkExists("dev2", idDev2) // just created
      checkExists("dev1", idDev1) // still exists

      Thread.sleep(5 * TimeUnit) // T10timeunit

      // retention of 10 time units

      Thread.sleep(2 * TimeUnit) // T12timeunit

      checkDoesNotExist("dev1", idDev1) // cleaned up
      checkExists("dev2", idDev2) // still there

      Thread.sleep(6 * TimeUnit) // T17timeunit

      checkDoesNotExist("dev2", idDev2) // cleaned up too

    }

  }

  private def jsonAs[T](json: String)(implicit v: Decoder[T]): T = {
    parse(json).toOption.flatMap(_.as[T].toOption).getOrElse(throw new IllegalArgumentException(s"Cannot parse $json"))
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
    System.clearProperty("config-dir")
    ConfigFactory.invalidateCaches() // force reload of java properties
    assertThrows[ConfigReaderException[Args]](Server.run(List.empty[String]).unsafeRunSync())
  }

  private def launchAsync(): Thread = {
    val runnable = new Runnable() {
      override def run() = {
        Server.main(Array.empty[String])
      }
    }
    val thread = new Thread(runnable)
    thread.start()
    thread
  }

}
