package org.mauritania.main4ino.models

import org.mauritania.main4ino.models.ActorMap.ActorMap
import org.mauritania.main4ino.models.Device.Metadata
import org.mauritania.main4ino.models.Device.Metadata.Status

case class Device(
  metadata: Metadata,
  actors: ActorMap = Device.EmptyActorMap
) {

  def asActorTups: Iterable[ActorTup] =
    for {
      (actor, ps) <- actors.toSeq
      (propName, propValue) <- ps.toSeq
    } yield (ActorTup(None, actor, propName, propValue, None))

  def actor(s: ActorName): Option[PropsMap] = actors.get(s)

}

object Device {

  val EmptyActorMap: ActorMap = Map.empty[ActorName, PropsMap]

  /**
    * It represents a request that relates to a specific device
    */
  case class Metadata(
    id: Option[RecordId],
    creation: Option[EpochSecTimestamp],
    device: DeviceName,
    status: Status
  )

  object Metadata {

    type Status = String // TODO have type safety here

    object Status {
      // Natural progression: O -> C -> X
      val Open: Status = "O" // modifications can be done on it (add properties)
      val Closed: Status = "C" // modifications cannot be done on it anymore
      val Consumed: Status = "X" // it has been already consumed (no modifications can be done on it)
    }

  }

  def apply(dev: DeviceName, ts: EpochSecTimestamp, am: ActorMap): Device = {
    val status = if (am.isEmpty) Status.Open else Status.Closed
    Device(Metadata(None, Some(ts), dev, status), am)
  }


}

