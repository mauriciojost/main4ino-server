package org.mauritania.main4ino

import cats.effect.{ConcurrentEffect, IO, Sync}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

trait HttpClient {

  def withHttpClient[T](f: Client[IO] => T): T = {
    val ec = Helper.testExecutionContext
    val cs = IO.contextShift(ec)
    val re = BlazeClientBuilder[IO](ec)(IO.ioConcurrentEffect(cs)).resource
    re.use(c => IO(f(c))).unsafeRunSync()
  }
}
