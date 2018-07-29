package org.mauritania.botinobe.api.v1

import org.mauritania.botinobe.api.v1.DeviceU.{ActorMapU, MetadataU}
import org.mauritania.botinobe.models.Device.Metadata
import org.mauritania.botinobe.models._

case class DeviceU(
  metadata: MetadataU,
  actors: ActorMapU
) {

  def toBom: Device = {
    Device(
      metadata = Metadata(
        id = metadata.id,
        timestamp = metadata.timestamp,
        device = metadata.device
      ),
      actors = actors.mapValues(_.mapValues((_, Status.Created)))
    )

  }

}

object DeviceU {

  type ActorMapU = Map[ActorName, Map[PropName, PropValue]]

  val EmptyActorMapU: ActorMapU = Map.empty[ActorName, Map[PropName, PropValue]]

  case class MetadataU (
    id: Option[RecordId],
    timestamp: Option[Timestamp],
    device: DeviceName
  )

  def fromBom(b: Device): DeviceU = {
    DeviceU(
      metadata = MetadataU(
        id = b.metadata.id,
        timestamp = b.metadata.timestamp,
        device = b.metadata.device
      ),
      actors = b.actors.mapValues(_.mapValues(_._1))
    )

  }


}

