package org.mauritania.main4ino.models

import java.time.ZonedDateTime

import org.mauritania.main4ino.models.ActorTup.Status
import org.mauritania.main4ino.models.PropsMap.PropsMap

case class ActorTup(
	requestId: Option[RecordId],
	device: DeviceName,
	actor: ActorName,
	prop: PropName,
	value: PropValue,
	status: Status,
  creation: Option[EpochSecTimestamp]
) {
	def withRequestId(i: Option[RecordId]): ActorTup = this.copy(requestId = i) // TODO get rid of this .copy by using composition of ActorTupIdless
	override def toString(): String = {
		s"${this.getClass.getSimpleName}(id:${requestId.mkString}, device:$device, actor:$actor, prop:$prop, value:$value, status:$status)"
	}
}

object ActorTup {

	type Status = String // TODO must use a proper type

	object Status {
		val Created: Status = "C"
		val Consumed: Status = "X"
	}

	def fromPropsMap(requestId: RecordId, deviceName: DeviceName, actorName: ActorName, pm: PropsMap, ts: EpochSecTimestamp): Iterable[ActorTup] = {
		pm.map { case (name, (value, status)) =>
			ActorTup(Some(requestId), deviceName, actorName, name, value, status, Some(ts))
		}
	}
}


