package org.mauritania.main4ino

import cats.effect.{Fiber, IO}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object Scheduler {

  def periodicIO[F](task: IO[F], duration: FiniteDuration)(implicit ec: ExecutionContext): IO[Fiber[IO, F]] = {
    def repeat: IO[F] = {
      for {
        _ <- IO.sleep(duration)
        p <- task
        r <- repeat
      } yield (r)
    }

    repeat.start
  }

}
