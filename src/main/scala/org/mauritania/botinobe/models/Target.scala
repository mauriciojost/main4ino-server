package org.mauritania.botinobe.models

import org.mauritania.botinobe.models.Target.Metadata

case class Target(
	metadata: Metadata,
	props: ActorPropsMap = Target.EmptyActorsPropMap
) {

	def asProps: Iterable[Prop] =
		for {
			(actor, ps) <- props.toSeq
			(propName, propValue) <- ps.toSeq
		} yield (Prop(actor, propName, propValue))

	def asTuples: Iterable[(ActorName, PropName, Option[Timestamp], PropValue)] = {
		this.asProps.map(p => (p.actor, p.prop, metadata.timestamp, p.value))
	}

}

object Target {

	val EmptyActorsPropMap: ActorPropsMap = Map.empty[ActorName, Map[PropName, PropValue]]

	case class Metadata (
		status: Status,
		device: DeviceName,
		timestamp: Option[Timestamp]
	)

	def fromProps(metadata: Metadata, ps: Iterable[Prop]): Target = {
		Target(metadata, Prop.asActorPropsMap(ps))
	}

	def merge(d: DeviceName, s: Status, targets: Seq[Target]): Seq[Target] = {
		val ts = targets.filter(t => t.metadata.device == d && t.metadata.status == s)
		val actorPropTimestampValues = tuple4Seq2MapOfMaps(ts.flatMap(_.asTuples))
		val actorLastProps = for {
			(aName, props) <- actorPropTimestampValues
			(pName, timestampValue) <- props
			lastValue <- Seq(timestampValue.maxBy(_._1)._2)
		} yield Prop(aName, pName, lastValue)

		if (actorLastProps.isEmpty) {
			Seq.empty[Target]
		} else {
			Seq(Target.fromProps(Metadata(Status.Merged, d, None), actorLastProps))
		}
	}

	private[models] def tuple4Seq2MapOfMaps[A,B,C, D](seq: Seq[(A,B,C,D)]): Map[A,Map[B,Map[C,D]]] =
    seq.groupBy(_._1).
      mapValues(_.groupBy(_._2).
        mapValues(_.groupBy(_._3).
          mapValues{ a => a.head._4 })) // at this point there is only 1 element

}

