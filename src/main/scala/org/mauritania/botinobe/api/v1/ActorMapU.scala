package org.mauritania.botinobe.api.v1

import org.mauritania.botinobe.api.v1.PropsMapU.PropsMapU
import org.mauritania.botinobe.models.ActorMap.ActorMap
import org.mauritania.botinobe.models._

object ActorMapU {

  type ActorMapU = Map[ActorName, PropsMapU]

  def fromTups(ps: Iterable[ActorTup]): ActorMapU = fromActorMap(ActorMap.fromTups(ps))

  def fromActorMap(a: ActorMap): ActorMapU = a.mapValues(_.mapValues(_._1))

}
