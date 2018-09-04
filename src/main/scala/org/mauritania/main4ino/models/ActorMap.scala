package org.mauritania.main4ino.models

import org.mauritania.main4ino.models.PropsMap.PropsMap

object ActorMap {

  type ActorMap = Map[ActorName, PropsMap]

  // assumed ActorTup from the same device
  def fromTups(ps: Iterable[ActorTup]): ActorMap = {
    val props = ps.groupBy(_.actor)
      .mapValues(_.groupBy(_.prop)
        .mapValues{a => {
          val mx = a.maxBy(_.requestId)
          (mx.value, mx.status)
        }})
    props
  }

}
