package org.mauritania.botinobe

package object models {

	type Status = String

	type RecordId = Long

	type DeviceName = String
	type ActorName = String

	type PropName = String
	type PropValueStatus = (String, Status)

	type ActorMap = Map[ActorName, Map[PropName, PropValueStatus]]

	type Timestamp = Long

}
