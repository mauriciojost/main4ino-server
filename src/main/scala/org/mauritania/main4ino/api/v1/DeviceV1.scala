package org.mauritania.main4ino.api.v1

import org.mauritania.main4ino.api.v1.ActorMapV1.ActorMapV1
import org.mauritania.main4ino.api.v1.DeviceV1.MetadataV1
import org.mauritania.main4ino.models.ActorTup.{Status => AtStatus}
import org.mauritania.main4ino.models.Device.Metadata.{Status => MdStatus}
import org.mauritania.main4ino.models.Device.Metadata
import org.mauritania.main4ino.models._

case class DeviceV1(
  metadata: MetadataV1,
  actors: ActorMapV1
) {

  def toBom: Device = {
    Device(
      metadata = Metadata(
        id = metadata.id,
        creation = metadata.timestamp,
        device = metadata.device,
        status = metadata.status
      ),
      actors = actors.mapValues(_.mapValues((_, AtStatus.Created)))
    )

  }

}

object DeviceV1 {

  val EmptyActorMapV1: ActorMapV1 = Map.empty[ActorName, Map[PropName, PropValue]]

  case class MetadataV1 (
    id: Option[RecordId],
    timestamp: Option[EpochSecTimestamp],
    device: DeviceName,
    status: MdStatus
  )

  def fromBom(b: Device): DeviceV1 = {
    DeviceV1(
      metadata = MetadataV1(
        id = b.metadata.id,
        timestamp = b.metadata.creation,
        device = b.metadata.device,
        status = b.metadata.status
      ),
      actors = b.actors.mapValues(_.mapValues(_._1))
    )

  }


}

