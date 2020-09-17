package org.mauritania.main4ino.db

import cats.effect.{Async, ConcurrentEffect, ContextShift, Sync, Timer}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.mauritania.main4ino.db.Repository.{ReqType, Stats}
import org.mauritania.main4ino.helpers.Time
import cats.effect._
import cats.implicits._
import eu.timepit.refined.types.numeric.PosInt

class Cleaner[F[_]: ContextShift: ConcurrentEffect: Timer: Sync: Async](
  repo: Repository[F],
  time: Time[F]
) {

  def cleanupRepo(retentionSecs: PosInt): F[Int] = {
    for {
      logger <- Slf4jLogger.fromClass[F](getClass)
      now <- time.nowUtc
      epSecs = now.toEpochSecond
      statsBef <- repo.stats()
      reportsCleaned <- repo.cleanup(ReqType.Reports, epSecs, retentionSecs)
      targetsCleaned <- repo.cleanup(ReqType.Targets, epSecs, retentionSecs)
      statsAft <- repo.stats()
      _ <- logger.info(
        s"Repository cleanup at $now ($epSecs): (reports)$reportsCleaned+(targets)$targetsCleaned requests cleaned"
      )
      _ <- logger.info(s"Stats before: $statsBef")
      _ <- logger.info(s"Stats after : $statsAft")
    } yield (reportsCleaned + targetsCleaned)
  }

}
