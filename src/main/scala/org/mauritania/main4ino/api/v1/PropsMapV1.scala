package org.mauritania.main4ino.api.v1

import org.mauritania.main4ino.api.v1.ActorMapV1.ActorMapV1
import org.mauritania.main4ino.models.ActorTup.Status
import org.mauritania.main4ino.models.PropsMap.PropsMap
import org.mauritania.main4ino.models.{ActorTup, PropName, PropValue}

object PropsMapV1 {

  type PropsMapV1 = Map[PropName, PropValue]

  def fromActorMapV1(m: ActorMapV1): PropsMapV1 = m.map(_._2).fold(Map.empty[PropName, PropValue])(_++_) // assumes all properties belong to only one actor

  def fromTups(ps: Iterable[ActorTup]): PropsMapV1 = fromActorMapV1(ActorMapV1.fromTups(ps))

  def toPropsMap(p: PropsMapV1, s: Status): PropsMap = p.mapValues(v => (v, s))

}
