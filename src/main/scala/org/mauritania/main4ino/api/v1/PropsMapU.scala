package org.mauritania.main4ino.api.v1

import org.mauritania.main4ino.api.v1.ActorMapU.ActorMapU
import org.mauritania.main4ino.models.{ActorTup, PropName, PropValue}

object PropsMapU {

  type PropsMapU = Map[PropName, PropValue]

  def fromActorMapU(m: ActorMapU): PropsMapU = m.map(_._2).fold(Map.empty[PropName, PropValue])(_++_) // assumes all properties belong to only one actor

  def fromTups(ps: Iterable[ActorTup]): PropsMapU = fromActorMapU(ActorMapU.fromTups(ps))

}
