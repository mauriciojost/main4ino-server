package org.mauritania.main4ino

import cats.effect.IO
import org.http4s.{InvalidBodyException, Status}
import org.http4s.client.{Client, UnexpectedStatus}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, Sequential}
import org.http4s.client.blaze.Http1Client

class ServerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  Sequential
  var appThread: Thread = _
  var httpClient: Client[IO] = _

  val Token = "token/012345678901234567890123456789"

  override def beforeAll(): Unit = {
    appThread = launchAsync(Array())
    httpClient = Http1Client[IO]().unsafeRunSync
    Thread.sleep(3000)
  }

  override def afterAll(): Unit = {
    appThread.interrupt()
    httpClient.shutdownNow()
  }

  "The server" should "start and expose the webapp files" in {
    val help = httpClient.expect[String](s"http://localhost:8080/api/v1/$Token/help")
    help.unsafeRunSync() should include("HELP")

    assertThrows[UnexpectedStatus]{
      httpClient.expect[String]("http://localhost:8080/api/v1/help").unsafeRunSync()
    }
  }

  it should "start and expose the rest api (v1)" in {
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
