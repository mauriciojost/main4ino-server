package org.mauritania.botinobe.models

import org.mauritania.botinobe.models.Device.Metadata

case class Device(
	metadata: Metadata,
	actors: ActorMap = Device.EmptyAcPropsMap
) {

	def asActorTups: Iterable[ActorTup] =
		for {
			(actor, ps) <- actors.toSeq
			(propName, propValue) <- ps.toSeq
		} yield (ActorTup(actor, propName, propValue))

	def asTuples: Iterable[(ActorName, PropName, Option[Timestamp], PropValue)] = {
		this.asActorTups.map(p => (p.actor, p.prop, metadata.timestamp, p.value))
	}

	def withId(i: Option[RecordId]): Device = this.copy(metadata = this.metadata.copy(id = i))
	def withDeviceName(n: DeviceName): Device = this.copy(metadata = this.metadata.copy(device = n))
	def withStatus(s: Status): Device = this.copy(metadata = this.metadata.copy(status = s))

}

object Device {

	val EmptyAcPropsMap: ActorMap = Map.empty[ActorName, Map[PropName, PropValue]]

	case class Metadata (
		id: Option[RecordId],
		status: Status,
		device: DeviceName,
		timestamp: Option[Timestamp]
	)

	def fromActorTups(metadata: Metadata, ps: Iterable[ActorTup]): Device = {
		Device(metadata, ActorTup.asActorMap(ps))
	}

	def merge(d: DeviceName, s: Status, devices: Seq[Device]): Seq[Device] = {
		val ts = devices.filter(t => t.metadata.device == d && t.metadata.status == s)
		val actorPropTimestampValues = tuple4Seq2MapOfMaps(ts.flatMap(_.asTuples))
		val actorLastProps = for {
			(aName, props) <- actorPropTimestampValues
			(pName, timestampValue) <- props
			lastValue <- Seq(timestampValue.maxBy(_._1)._2)
		} yield ActorTup(aName, pName, lastValue)

		if (actorLastProps.isEmpty) {
			Seq.empty[Device]
		} else {
			Seq(Device.fromActorTups(Metadata(None, Status.Merged, d, None), actorLastProps))
		}
	}

	private[models] def tuple4Seq2MapOfMaps[A,B,C, D](seq: Seq[(A,B,C,D)]): Map[A,Map[B,Map[C,D]]] =
    seq.groupBy(_._1).
      mapValues(_.groupBy(_._2).
        mapValues(_.groupBy(_._3).
          mapValues{ a => a.head._4 })) // at this point there is only 1 element

}

