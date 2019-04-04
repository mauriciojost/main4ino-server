package org.mauritania.main4ino.models

import org.mauritania.main4ino.helpers.Time
import org.mauritania.main4ino.models.ActorMap.ActorMap
import org.mauritania.main4ino.models.Device.Metadata
import org.mauritania.main4ino.models.PropsMap.PropsMap

case class Device(
  metadata: Metadata,
  actors: ActorMap = Device.EmptyActorMap
) {

  def asActorTups: Iterable[ActorTup] =
    for {
      (actor, ps) <- actors.toSeq
      (propName, (propValue, status)) <- ps.toSeq
    } yield (ActorTup(None, metadata.device, actor, propName, propValue, status))

  override def toString(): String = {
    this.getClass.getSimpleName + "(\n" +
      "  id       : " + metadata.id.mkString + "\n" +
      "  device   : " + metadata.device + "\n" +
      "  timestamp: " + metadata.timestamp.map(Time.asString).mkString + "\n" +
      "  actors   : \n" + actors.map { case (a, p) => s"    - $a: ${p.map { case (k, (v, s)) => s"$k=$v($s)" }.mkString(", ")} \n" }.mkString +
      ")"
  }
}

object Device {

  val EmptyActorMap: ActorMap = Map.empty[ActorName, PropsMap]

  case class Metadata(
    id: Option[RecordId],
    timestamp: Option[EpochSecTimestamp],
    device: DeviceName
  )

  /**
    * Intermediate device representation
    *
    * It contains a single actor tuple. Instances of this class
    * are meant to be aggregated to create instances of [[Device]].
    *
    * @param metadata   the metadata
    * @param actorTuple the single actor tuple
    */
  case class Device1(
    metadata: Metadata,
    actorTuple: ActorTup
  )

  def fromActorTups(metadata: Metadata, ps: Iterable[ActorTup]): Device = Device(metadata, ActorMap.fromTups(ps))

  def fromDevice1s(s: Iterable[Device1]): Iterable[Device] = {
    val g = s.groupBy(_.metadata)
    val ds = g.map { case (md, d1s) => Device(md, ActorMap.fromTups(d1s.map(_.actorTuple))) }
    ds
  }


}

