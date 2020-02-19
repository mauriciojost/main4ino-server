package org.mauritania.main4ino.db

import cats.effect.{Async, ConcurrentEffect, ContextShift, Sync, Timer}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.mauritania.main4ino.db.Repository.ReqType
import org.mauritania.main4ino.helpers.Time
import cats.effect._
import cats.implicits._
import eu.timepit.refined.types.numeric.PosInt

class Cleaner[F[_]: ContextShift: ConcurrentEffect: Timer: Sync: Async](repo: Repository[F], time: Time[F]) {

  def cleanupRepo(retentionSecs: PosInt) = {
    for {
      logger <- Slf4jLogger.fromClass[F](getClass)
      now <- time.nowUtc
      epSecs = now.toEpochSecond
      reportsCleaned <- repo.cleanup(ReqType.Reports, epSecs, retentionSecs)
      targetsCleaned <- repo.cleanup(ReqType.Targets, epSecs, retentionSecs)
      _ <- logger.info(s"Repository cleanup at $now ($epSecs): (reports)$reportsCleaned+(targets)$targetsCleaned requests cleaned")
    } yield (reportsCleaned + targetsCleaned)
  }

}
