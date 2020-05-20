package org.mauritania.main4ino

import java.io.File

import cats.effect.{ContextShift, ExitCode, Fiber, IO}
import com.typesafe.config.ConfigFactory
import org.http4s.client.UnexpectedStatus
import org.http4s.{BasicCredentials, Method, Request, Status, Uri}
import org.scalatest.{Assertion, BeforeAndAfterAll}
import org.scalatest.Assertions._
import io.circe.syntax._
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe._
import io.circe.parser._
import me.alexpanov.net.FreePortFinder
import org.mauritania.main4ino.api.v1.JsonEncoding
import org.mauritania.main4ino.api.Translator.IdResponse
import org.mauritania.main4ino.helpers.ConfigLoader
import org.mauritania.main4ino.models.{DeviceName, RequestId}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pureconfig.error.ConfigReaderException

class ServerSpec extends AnyFlatSpec with Matchers with HttpClient with BeforeAndAfterAll {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(Helper.testExecutionContext)

  var port: Int = _
  final private val ConfigDirPath = "src/test/resources/configs/1"
  final private val DynamicPortBeginning = 49152

  final private lazy val appFiber = launchAppAsync()
  final private val UserPass = BasicCredentials(Fixtures.User1.id, Fixtures.User1Pass)

  implicit final private val statusEncoder = JsonEncoding.StatusEncoder
  implicit final private val statusDecoder = JsonEncoding.StatusDecoder

  override def beforeAll(): Unit = {
    port = FreePortFinder.findFreeLocalPort(DynamicPortBeginning)
    appFiber.join.unsafeRunAsyncAndForget()
    while (FreePortFinder.available(port)) {
      Thread.sleep(100)
    };
  }

  override def afterAll(): Unit = {
    appFiber.cancel.unsafeRunSync()
  }

  "The server" should "start and expose rest the api (v1)" in {
    withHttpClient { httpClient =>
      val help = httpClient.expect[String](s"http://localhost:$port/api/v1/token/${UserPass.token}/help")
      help.unsafeRunSync() should include("https")
    }
  }

  it should "reject unauthorized requests" in {
    withHttpClient { httpClient =>
      assertThrows[UnexpectedStatus] { // forbidden
        httpClient.expect[String](s"http://localhost:$port/api/v1/help").unsafeRunSync()
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

      def checkRecord(dev: DeviceName, id: Long, status: Status, errMsg: String): Assertion =
       assert(httpClient.status(devGetRequest(dev, "targets", id)).unsafeRunSync() === status, errMsg)
      def checkRecordExists(dev: DeviceName, id: Long, errMsg: String): Assertion = checkRecord(dev, id, Status.Ok, errMsg)
      def checkRecordDoesNotExist(dev: DeviceName, id: Long, errMsg: String): Assertion = checkRecord(dev, id, Status.NoContent, errMsg)
      val timeUnitSecs = config.database.cleanup.retentionSecs.value.toFloat / 10 // a 10th fraction of the retention
      def sleepTimeUnits(tu: Float): Unit = Thread.sleep((tu * 1000 * timeUnitSecs).toLong)

      // T0, inject dev1 (at ~T0, its cleanup should take place at ~T10)
      val dev1ResponseJson = httpClient.expect[String](devPostRequest("dev1", "targets"))
      val idDev1 = jsonAs[IdResponse](dev1ResponseJson.unsafeRunSync()).id

      sleepTimeUnits(5) // T05

      checkRecordExists("dev1", idDev1, "dev1 just created, should exist")

      sleepTimeUnits(15) // T20

      // T20, inject dev2 (at ~T20, its cleanup should take place at ~T30)
      val dev2ResponseJson = httpClient.expect[String](devPostRequest("dev2", "targets"))
      val idDev2 = jsonAs[IdResponse](dev2ResponseJson.unsafeRunSync()).id

      checkRecordDoesNotExist("dev1", idDev1, "dev1 should have been cleaned up")
      checkRecordExists("dev2", idDev2, "dev2 just created, should exist")

      sleepTimeUnits(20) // T40

      checkRecordDoesNotExist("dev1", idDev1, "dev1 should have been cleaned up (way before)")
      checkRecordDoesNotExist("dev2", idDev2, "dev2 should have been cleaned up")

    }

  }

  private def jsonAs[T](json: String)(implicit v: Decoder[T]): T = {
    parse(json).toOption.flatMap(_.as[T].toOption).getOrElse(throw new IllegalArgumentException(s"Cannot parse $json"))
  }

  private def devPostRequest(devName: String, table: String) = {
    Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString(s"http://localhost:$port/api/v1/token/${UserPass.token}/devices/$devName/$table"),
      body = Helper.asEntityBody[IO]("""{"actor1":{"prop1":"val1"}}""")
    )
  }

  private def devGetRequest(devName: String, table: String, id: RequestId) = {
    Request[IO](
      method = Method.GET,
      uri = Uri.unsafeFromString(s"http://localhost:$port/api/v1/token/${UserPass.token}/devices/$devName/$table/$id")
    )
  }

  it should "start and expose the webapp files" in {
    withHttpClient { httpClient =>
      val help = httpClient.expect[String](s"http://localhost:$port/index.html")
      help.unsafeRunSync() should include("</body>")
    }
  }

  it should "start and expose the firmware files" in {
    withHttpClient { httpClient =>
      val help = httpClient.expect[String](s"http://localhost:$port/firmwares/botino/esp8266")
      help.unsafeRunSync() should include("1.0.0")
    }
  }

  it should "fail if started with bad arduments" in {
    System.clearProperty("config-dir")
    ConfigFactory.invalidateCaches() // force reload of java properties
    assertThrows[ConfigReaderException[Args]](Server.run(List.empty[String]).unsafeRunSync())
  }

  private def launchAppAsync(): Fiber[IO, ExitCode] = {
    System.setProperty("config-dir", ConfigDirPath)
    System.setProperty("server.port", port.toString)
    ConfigFactory.invalidateCaches() // force reload of java properties
    Server.run(List.empty[String]).start.unsafeRunSync()
  }

}
