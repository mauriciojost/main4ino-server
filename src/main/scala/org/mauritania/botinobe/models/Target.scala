package org.mauritania.botinobe.models

import org.mauritania.botinobe.Expandable
import org.mauritania.botinobe.models.Target.Metadata

case class Target(
	metadata: Metadata,
	props: ActorPropsMap
) extends Expandable

object Target {

	// TODO: create a dedicated ADT for this
	type Status = String
	val Created: Status = "created"
	val Read: Status = "read"

	case class Metadata (
		status: Status,
		device: DeviceName
	)

	def fromListOfProps(metadata: Metadata, ps: List[Prop]): Target = {
		val byActor: Map[ActorName, List[Prop]] = ps.groupBy(_.actor)
		val props: ActorPropsMap = byActor.mapValues(_.map(i => (i.value, i.prop)).toMap)
		Target(metadata, props)
	}
}

