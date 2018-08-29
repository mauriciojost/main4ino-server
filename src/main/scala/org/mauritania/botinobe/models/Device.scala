package org.mauritania.botinobe.models

import org.mauritania.botinobe.models.ActorMap.ActorMap
import org.mauritania.botinobe.models.Device.Metadata
import org.mauritania.botinobe.models.PropsMap.PropsMap

case class Device(
	metadata: Metadata,
	actors: ActorMap = Device.EmptyActorMap
) {

	def asActorTups: Iterable[ActorTup] =
		for {
			(actor, ps) <- actors.toSeq
			(propName, (propValue, status)) <- ps.toSeq
		} yield (ActorTup(None, metadata.device, actor, propName, propValue, status))

}

object Device {

	val EmptyActorMap: ActorMap = Map.empty[ActorName, PropsMap]

	case class Metadata ( // TODO metadata information that comes after DB insertion should be put on a wrapper of Device to avoid using .copy
		id: Option[RecordId],
		timestamp: Option[Timestamp],
    device: DeviceName
	)

	def fromActorTups(metadata: Metadata, ps: Iterable[ActorTup]): Device = Device(metadata, ActorMap.fromTups(ps))

}

