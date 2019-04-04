package org.mauritania.main4ino

package object models {

	type Status = String // TODO must use a proper type

	object Status {
		val Created: Status = "C"
		val Consumed: Status = "X"
	}

	type RecordId = Long // TODO change to RequestId

	type DeviceName = String
	type ActorName = String

	type PropName = String
	type PropValue = String

	type EpochSecTimestamp = Long

}
