package org.mauritania.botinobe.models

import org.mauritania.botinobe.helpers.Expandable
import org.mauritania.botinobe.models.Target.Metadata
import scalaz.Scalaz._

case class Target(
	metadata: Metadata,
	props: ActorPropsMap
) extends Expandable {

	import Target._

	def ++(o: Target): Target = {
		assert(metadata.device == o.metadata.device)
		val ps = o.props |+| props
		Target(Metadata(Merged, metadata.device), ps)

	}
}

object Target {

	// TODO: create a dedicated ADT for this
	type Status = String
	val Created: Status = "created"
	val Consumed: Status = "consumed"
	val Merged: Status = "merged"

	case class Metadata (
		status: Status,
		device: DeviceName
	)

	def fromListOfProps(metadata: Metadata, ps: Iterable[Prop]): Target = {
		val byActor: Map[ActorName, Iterable[Prop]] = ps.groupBy(_.actor)
		val props: ActorPropsMap = byActor.mapValues(_.map(i => (i.value, i.prop)).toMap)
		Target(metadata, props)
	}

	def merge(d: DeviceName, s: Status, targets: Seq[Target]): Seq[Target] = {
		val ts = targets.filter(t => t.metadata.device == d && t.metadata.status == s)
		val tuples = for {
			t <- ts
			(aName, p) <- t.props
			(pName, pValue) <- p
		} yield((aName, pName, 1L, pValue)) // given an actor name, get the properties sorted by timestamp
		// TODO add timestamp to target metadata to sort above

		val actorPropTimestampValues = tuple4Seq2MapOfMaps(tuples)
		val actorLastProps = for {
			(aName, props) <- actorPropTimestampValues
			(pName, timestampValue) <- props
			lastValue <- Seq(timestampValue.maxBy(_._1)._2)
		} yield Prop(aName, pName, lastValue)

		if (actorLastProps.size > 0) {
			Seq(Target.fromListOfProps(Metadata(s, d), actorLastProps))
		} else {
			Seq.empty[Target]
		}

	}

	private def tuple4Seq2MapOfMaps[A,B,C, D](seq: Seq[(A,B,C,D)])(implicit o: Ordering[(A, B, C, D)]): Map[A,Map[B,Map[C,D]]] =
    seq.groupBy(_._1).
      mapValues(_.groupBy(_._2).
          mapValues(_.groupBy(_._3).
            mapValues{ a => a.max(o)._4 }))

}

