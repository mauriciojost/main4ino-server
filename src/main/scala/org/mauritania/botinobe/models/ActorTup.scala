package org.mauritania.botinobe.models

case class ActorTup(
	requestId: Option[RecordId],
	device: DeviceName,
	actor: ActorName,
	prop: PropName,
	value: PropValue,
	status: Status
) {
	def withRequestId(i: Option[RecordId]): ActorTup = this.copy(requestId = i)
}


