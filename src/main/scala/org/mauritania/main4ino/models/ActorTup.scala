package org.mauritania.main4ino.models

import org.mauritania.main4ino.models.PropsMap.PropsMap

case class ActorTup(
	requestId: Option[RecordId],
	device: DeviceName,
	actor: ActorName,
	prop: PropName,
	value: PropValue,
	status: Status
) {
	def withRequestId(i: Option[RecordId]): ActorTup = this.copy(requestId = i) // TODO get rid of this .copy by using composition of ActorTupIdless
	override def toString(): String = {
		s"${this.getClass.getSimpleName}(id:${requestId.mkString}, device:$device, actor:$actor, prop:$prop, value:$value, status:$status)"
	}
}

object ActorTup {

	def fromPropsMap(requestId: RecordId, deviceName: DeviceName, actorName: ActorName, pm: PropsMap): Iterable[ActorTup] = {
		pm.map { case (name, (value, status)) =>
			ActorTup(Some(requestId), deviceName, actorName, name, value, status)
		}
	}
}


