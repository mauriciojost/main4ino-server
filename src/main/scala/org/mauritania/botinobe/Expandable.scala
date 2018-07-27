package org.mauritania.botinobe

import org.mauritania.botinobe.models.{Prop, ActorPropsMap}


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
