package org.mauritania.botinobe.models

case class ActorTup(
	id: Option[RecordId],
	device: DeviceName,
	actor: ActorName,
	prop: PropName,
	value: PropValue,
	status: Status
) {
	def withId(i: Option[RecordId]): ActorTup = this.copy(id = i)
}

object ActorTup {

	// assumed ActorTup from the same device
	def asActorMap(ps: Iterable[ActorTup]): ActorMap = {
		val props = ps.groupBy(_.actor)
			.mapValues(_.groupBy(_.prop)
				.mapValues{a => {
					val mx = a.maxBy(_.id)
					(mx.value, mx.status)
        }})
		props
	}
}

