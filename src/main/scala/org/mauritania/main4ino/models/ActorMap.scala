package org.mauritania.main4ino.models

import org.mauritania.main4ino.models.PropsMap.PropsMap

object ActorMap {

  type ActorMap = Map[ActorName, PropsMap]

  /**
    * Merge several [[ActorTup]] of the same device
    *
    * Perform grouping of all [[ActorTup]] belonging by actor-property and
    * keep the one provided in the latest request ID.
    *
    * @param actorTups all [[ActorTup]] to be merged belonging to the same device
    * @return a merged [[ActorMap]] with only one value per actor-property
    */
  def resolveFromTups(actorTups: Iterable[ActorTup]): ActorMap = {
    val props = actorTups.groupBy(_.actor)
      .mapValues { byActorActorTups =>
        byActorActorTups.groupBy(_.prop)
          .mapValues { byActorPropActorTups =>
            val eligibleActorTupPerActorProp = byActorPropActorTups.maxBy(_.requestId)
            (eligibleActorTupPerActorProp.value, eligibleActorTupPerActorProp.status)
          }
      }
    props
  }

}
