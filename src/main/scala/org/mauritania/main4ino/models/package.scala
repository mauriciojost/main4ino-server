package org.mauritania.main4ino

package object models {

	type RecordId = Long // TODO change to RequestId

	type DeviceName = String
	type ActorName = String

	type PropName = String
	type PropValue = String

	type PropsMap = Map[PropName, PropValue]

	type EpochSecTimestamp = Long

}
