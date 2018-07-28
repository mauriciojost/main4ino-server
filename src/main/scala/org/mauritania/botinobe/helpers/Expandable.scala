package org.mauritania.botinobe.helpers

import org.mauritania.botinobe.models.{ActorPropsMap, Prop}

trait Expandable {
	this: {
		val props: ActorPropsMap
	} =>

	def expandProps: Iterable[Prop] =
		for {
			(actor, props) <- props.toSeq
			(propName, propValue) <- props.toSeq
		} yield (Prop(actor, propName, propValue))

}
