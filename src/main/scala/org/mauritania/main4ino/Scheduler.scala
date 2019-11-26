package org.mauritania.main4ino

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.MILLISECONDS
import cats.effect.Clock
import cats.effect.Timer
import cats.effect.Sync
import cats.implicits._

object Scheduler {
  def periodic[F[_] : Sync : Timer, A](interval: FiniteDuration, task: F[A]): F[A] = for {
    m <- measure(task)
    (_, elapsed) = m
    remaining = interval - elapsed
    _ <- Timer[F].sleep(remaining)
    result <- periodic(interval, task)
  } yield result


  def measure[F[_] : Sync : Clock, A](fa: F[A]): F[(A, FiniteDuration)] = for {
    start <- Clock[F].monotonic(MILLISECONDS)
    result <- fa
    finish <- Clock[F].monotonic(MILLISECONDS)
  } yield (result, FiniteDuration(finish - start, MILLISECONDS))
}
