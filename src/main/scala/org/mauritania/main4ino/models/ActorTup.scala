package org.mauritania.main4ino.models

case class ActorTup(
	requestId: Option[RecordId],
	device: DeviceName,
	actor: ActorName,
	prop: PropName,
	value: PropValue,
	status: Status
) {
	def withRequestId(i: Option[RecordId]): ActorTup = this.copy(requestId = i) // TODO get rid of this .copy by using composition of ActorTupIdless
}


