package org.mauritania.botinobe.models

import org.mauritania.botinobe.models.Device.Metadata

case class Device(
	metadata: Metadata,
	actors: ActorMap = Device.EmptyActorMap
) {

	def asActorTups: Iterable[ActorTup] =
		for {
			(actor, ps) <- actors.toSeq
			(propName, (propValue, status)) <- ps.toSeq
		} yield (ActorTup(None, metadata.device, actor, propName, propValue, status))

	def asTuples: Iterable[ActorTup] = {
		this.asActorTups.map(p => ActorTup(metadata.id, metadata.device, p.actor, p.prop, p.value, p.status))
	}

	def withId(i: Option[RecordId]): Device = this.copy(metadata = this.metadata.copy(id = i))
	def withDeviceName(n: DeviceName): Device = this.copy(metadata = this.metadata.copy(device = n))
	def withStatus(s: Status): Device = Device.fromActorTups(metadata, asActorTups.map(_.copy(status = s)))
	def withTimestamp(t: Option[Timestamp]): Device = this.copy(metadata = this.metadata.copy(timestamp = t))

}

object Device {

	val EmptyActorMap: ActorMap = Map.empty[ActorName, Map[PropName, (PropValue, Status)]]

	case class Metadata (
		id: Option[RecordId],
		timestamp: Option[Timestamp],
    device: DeviceName
	)

	def fromActorTups(metadata: Metadata, ps: Iterable[ActorTup]): Device = Device(metadata, ActorTup.asActorMap(ps))

	def merge(ds: Seq[Device]): Seq[Device] = {
		val onlyCreated = ds.flatMap(_.asTuples).filter(_.status == Status.Created)
		val byDevActorProp = onlyCreated.groupBy(a => (a.device, a.actor, a.prop)).values
		val fresherById = byDevActorProp.map(dap => dap.maxBy(_.id))
    fresherById.groupBy(_.device).map{ case (dName, t) =>
      Device.fromActorTups(Metadata(None, None, dName), t)
    }.toSeq
	}

}

