package org.mauritania.main4ino

import java.util.concurrent.Executors

import cats.effect.{Fiber, IO}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object Scheduler {

  implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  def regularIO(task: IO[_], duration: FiniteDuration): IO[Fiber[IO, Unit]] = {
    def repeat: IO[Unit] = {
      for {
        _ <- IO.sleep(duration)
        p <- task
        _ <- IO.shift
        _ <- repeat
      } yield ()
    }

    repeat.start
  }

}
