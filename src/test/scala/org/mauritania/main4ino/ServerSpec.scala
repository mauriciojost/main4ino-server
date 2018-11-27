package org.mauritania.main4ino

import cats.effect.IO
import org.http4s.{BasicCredentials, Uri}
import org.http4s.client.{Client, UnexpectedStatus}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, Sequential}
import org.http4s.client.blaze.Http1Client
import org.mauritania.main4ino.security.Authentication

class ServerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  Sequential
  var appThread: Thread = _
  var httpClient: Client[IO] = _
  val UserPass = BasicCredentials(Fixtures.User1.id, Fixtures.User1Pass)

  override def beforeAll(): Unit = {
    appThread = launchAsync(Array())
    httpClient = Http1Client[IO]().unsafeRunSync
    Thread.sleep(3000)
  }

  override def afterAll(): Unit = {
    appThread.interrupt()
    httpClient.shutdownNow()
  }

  "The server" should "start and expose rest the api (v1)" in {
    val help = httpClient.expect[String](s"http://localhost:8080/api/v1/token/${UserPass.token}/help")
    help.unsafeRunSync() should include("HELP")

    assertThrows[UnexpectedStatus]{
      httpClient.expect[String]("http://localhost:8080/api/v1/help").unsafeRunSync()
    }
  }

  it should "start and expose the webapp files" in {
    val help = httpClient.expect[String](s"http://localhost:8080/index.html")
    help.unsafeRunSync() should include("</body>")
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
