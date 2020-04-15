package org.mauritania.main4ino

import java.io.File

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import org.http4s.client.UnexpectedStatus
import org.http4s.{BasicCredentials, Method, Request, Status, Uri}
import org.scalatest.{Assertion, BeforeAndAfterAll, ParallelTestExecution}
import io.circe.syntax._
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe._
import io.circe.parser._
import org.mauritania.main4ino.api.v1.JsonEncoding
import org.mauritania.main4ino.api.Translator.IdResponse
import org.mauritania.main4ino.helpers.ConfigLoader
import org.mauritania.main4ino.models.{DeviceName, RequestId}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pureconfig.error.ConfigReaderException

class ServerSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll with HttpClient with ParallelTestExecution {

  val InitializationTimeMs = 4000
  // configs/1/application.conf/database/cleanup/retention-secs
  // which must be retention-secs=(10 * timeUnitMs)
  val ConfigDirPath = "src/test/resources/configs/1"

  lazy val appThread: Thread = launchAsync()
  val UserPass = BasicCredentials(Fixtures.User1.id, Fixtures.User1Pass)

  implicit val statusEncoder = JsonEncoding.StatusEncoder
  implicit val statusDecoder = JsonEncoding.StatusDecoder

  override def beforeAll(): Unit = {
    System.setProperty("config-dir", ConfigDirPath)
    ConfigFactory.invalidateCaches() // force reload of java properties
    appThread
    Thread.sleep(InitializationTimeMs) // give time to the thread to initialize
  }

  override def afterAll(): Unit = {
    appThread.interrupt()
  }

  "The server" should "start and expose rest the api (v1)" in {
    withHttpClient { httpClient =>
      val help = httpClient.expect[String](s"http://localhost:18095/api/v1/token/${UserPass.token}/help")
      help.unsafeRunSync() should include("https")
    }
  }

  it should "reject unauthorized requests" in {
    withHttpClient { httpClient =>
      assertThrows[UnexpectedStatus] { // forbidden
        httpClient.expect[String]("http://localhost:18095/api/v1/help").unsafeRunSync()
      }
    }
  }

  it should "perform cleanup of old entries regularly" in {

    import ConfigLoader.PureConfigImplicits._
    import pureconfig._
    import pureconfig.generic.auto._
    import eu.timepit.refined.pureconfig._

    withHttpClient { httpClient =>

      val config = ConfigLoader.fromFile[IO, Config](new File(ConfigDirPath + "/application.conf")).unsafeRunSync()

      def checkRecord(dev: DeviceName, id: Long, status: Status): Assertion =
       httpClient.status(devGetRequest(dev, "targets", id)).unsafeRunSync() shouldBe status
      def checkRecordExists(dev: DeviceName, id: Long): Assertion = checkRecord(dev, id, Status.Ok)
      def checkRecordDoesNotExist(dev: DeviceName, id: Long): Assertion = checkRecord(dev, id, Status.NoContent)
      val timeUnitSecs = config.database.cleanup.retentionSecs.value.toFloat / 10 // a 10th fraction of the retention
      def sleepTimeUnits(tu: Float): Unit = Thread.sleep((tu * 1000 * timeUnitSecs).toLong)

      // T0, inject dev1 (at ~T0, its cleanup should take place at ~T10)
      val dev1ResponseJson = httpClient.expect[String](devPostRequest("dev1", "targets"))
      val idDev1 = jsonAs[IdResponse](dev1ResponseJson.unsafeRunSync()).id

      checkRecordExists("dev1", idDev1) // just created

      sleepTimeUnits(20) // T20

      checkRecordDoesNotExist("dev1", idDev1) // cleaned up

      // T20, inject dev2 (at ~T20, its cleanup should take place at ~T30)
      val dev2ResponseJson = httpClient.expect[String](devPostRequest("dev2", "targets"))
      val idDev2 = jsonAs[IdResponse](dev2ResponseJson.unsafeRunSync()).id

      checkRecordExists("dev2", idDev2) // just created

      sleepTimeUnits(20) // T40

      checkRecordDoesNotExist("dev2", idDev2) // cleaned up

    }

  }

  private def jsonAs[T](json: String)(implicit v: Decoder[T]): T = {
    parse(json).toOption.flatMap(_.as[T].toOption).getOrElse(throw new IllegalArgumentException(s"Cannot parse $json"))
  }

  private def devPostRequest(devName: String, table: String) = {
    Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString(s"http://localhost:18095/api/v1/token/${UserPass.token}/devices/$devName/$table"),
      body = Helper.asEntityBody[IO]("""{"actor1":{"prop1":"val1"}}""")
    )
  }

  private def devGetRequest(devName: String, table: String, id: RequestId) = {
    Request[IO](
      method = Method.GET,
      uri = Uri.unsafeFromString(s"http://localhost:18095/api/v1/token/${UserPass.token}/devices/$devName/$table/$id")
    )
  }

  it should "start and expose the webapp files" in {
    withHttpClient { httpClient =>
      val help = httpClient.expect[String](s"http://localhost:18095/index.html")
      help.unsafeRunSync() should include("</body>")
    }
  }

  it should "start and expose the firmware files" in {
    withHttpClient { httpClient =>
      val help = httpClient.expect[String](s"http://localhost:18095/firmwares/botino/esp8266")
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
