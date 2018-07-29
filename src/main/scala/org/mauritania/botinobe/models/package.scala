package org.mauritania.botinobe

package object models {

	type Status = String

	type RecordId = Long

	type DeviceName = String
	type ActorName = String

	type PropName = String
	type PropValue = String

	type ActorMap = Map[ActorName, Map[PropName, PropValue]]

	type Timestamp = Long

}
