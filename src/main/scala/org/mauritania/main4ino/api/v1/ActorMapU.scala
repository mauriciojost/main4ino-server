package org.mauritania.main4ino.api.v1

import org.mauritania.main4ino.api.v1.PropsMapU.PropsMapU
import org.mauritania.main4ino.models.ActorMap.ActorMap
import org.mauritania.main4ino.models._

object ActorMapU {

  type ActorMapU = Map[ActorName, PropsMapU]

  def fromTups(ps: Iterable[ActorTup]): ActorMapU = fromActorMap(ActorMap.fromTups(ps))

  def fromActorMap(a: ActorMap): ActorMapU = a.mapValues(_.mapValues(_._1))

}
