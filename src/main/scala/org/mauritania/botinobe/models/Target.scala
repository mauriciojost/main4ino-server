package org.mauritania.botinobe.models

import org.mauritania.botinobe.helpers.Expandable
import org.mauritania.botinobe.models.Target.Metadata

case class Target(
	metadata: Metadata,
	props: ActorPropsMap = Target.EmptyActorsPropMap
) extends Expandable

object Target {

	// TODO: create a dedicated ADT for this
	type Status = String
	val Created: Status = "created"
	val Consumed: Status = "consumed"
	val Merged: Status = "merged"

	val EmptyActorsPropMap: ActorPropsMap = Map.empty[ActorName, Map[PropName, PropValue]]

	case class Metadata (
		status: Status = Created,
		device: DeviceName,
    timestamp: Option[Timestamp] = None
	)

	def fromListOfProps(metadata: Metadata, ps: Iterable[Prop]): Target = {
		Target(metadata, Prop.asActorPropsMap(ps))
	}

	def merge(d: DeviceName, s: Status, targets: Seq[Target]): Seq[Target] = {
		val ts = targets.filter(t => t.metadata.device == d && t.metadata.status == s)
		val tuples = for {
			t <- ts
			Prop(aName, pName, pValue) <- t.expandProps
		} yield((aName, pName, t.metadata.timestamp, pValue))

		val actorPropTimestampValues = tuple4Seq2MapOfMaps(tuples)
		val actorLastProps = for {
			(aName, props) <- actorPropTimestampValues
			(pName, timestampValue) <- props
			lastValue <- Seq(timestampValue.maxBy(_._1.getOrElse(0L))._2)
		} yield Prop(aName, pName, lastValue)

		if (actorLastProps.size > 0) {
			Seq(Target.fromListOfProps(Metadata(Merged, d), actorLastProps))
		} else {
			Seq.empty[Target]
		}

	}

	private[models] def tuple4Seq2MapOfMaps[A,B,C, D](seq: Seq[(A,B,C,D)]): Map[A,Map[B,Map[C,D]]] =
    seq.groupBy(_._1).
      mapValues(_.groupBy(_._2).
        mapValues(_.groupBy(_._3).
          mapValues{ a => a.head._4 })) // at this point there is only 1 element

}

