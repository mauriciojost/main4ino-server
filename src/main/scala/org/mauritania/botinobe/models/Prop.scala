package org.mauritania.botinobe.models

case class Prop(
	actor: ActorName,
	prop: PropName,
	value: PropValue
)

object Prop {

	def asActorPropsMap(ps: Iterable[Prop]): ActorPropsMap = {
		val props = ps.groupBy(_.actor)
			.mapValues(_.groupBy(_.prop)
				.mapValues(_.head.value))
		props
	}
}

