package org.mauritania.main4ino

package object models {

	type RecordId = Long // TODO change to RequestId

	type DeviceName = String
	type ActorName = String

	// Properties name and value
	type PropName = String
	type PropValue = String

  // Properties of a given actor
	type PropsMap = Map[PropName, PropValue]

	// Properties of the actors of a device
	type ActorMap = Map[ActorName, PropsMap]

	// Timestamp in seconds from the epoch (in UTC)
	type EpochSecTimestamp = Long

}
