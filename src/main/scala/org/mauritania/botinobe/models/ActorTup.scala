package org.mauritania.botinobe.models

case class ActorTup(
	actor: ActorName,
	prop: PropName,
	value: PropValue
)

object ActorTup {

	def asActorMap(ps: Iterable[ActorTup]): ActorMap = {
		val props = ps.groupBy(_.actor)
			.mapValues(_.groupBy(_.prop)
				.mapValues(_.head.value))
		props
	}
}

