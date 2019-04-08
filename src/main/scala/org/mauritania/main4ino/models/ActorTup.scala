package org.mauritania.main4ino.models

case class ActorTup(
	requestId: Option[RecordId],
	actor: ActorName,
	prop: PropName,
	value: PropValue,
  creation: Option[EpochSecTimestamp]
) {
	def withRequestId(i: Option[RecordId]): ActorTup = this.copy(requestId = i) // TODO get rid of this .copy by using composition of ActorTupIdless
}

object ActorTup {

	/*
	type Status = String // TODO must use a proper type

	object Status {
    // Natural progression: C -> X
		val Created: Status = "C" // created, eligible for consumption
		val Consumed: Status = "X" // consumed, not eligible for consumption anymore
	}
	*/

	def fromPropsMap(requestId: RecordId, deviceName: DeviceName, actorName: ActorName, pm: PropsMap, ts: EpochSecTimestamp): Iterable[ActorTup] = {
		pm.map { case (name, value) =>
			ActorTup(Some(requestId), actorName, name, value, Some(ts))
		}
	}
}


