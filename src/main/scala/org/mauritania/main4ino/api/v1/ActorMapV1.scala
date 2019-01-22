package org.mauritania.main4ino.api.v1

import org.mauritania.main4ino.api.v1.PropsMapV1.PropsMapV1
import org.mauritania.main4ino.models.ActorMap.ActorMap
import org.mauritania.main4ino.models._

object ActorMapV1 {

  type ActorMapV1 = Map[ActorName, PropsMapV1]

  def fromTups(ps: Iterable[ActorTup]): ActorMapV1 = fromActorMap(ActorMap.fromTups(ps))

  def fromActorMap(a: ActorMap): ActorMapV1 = a.mapValues(_.mapValues(_._1))

}
