package org.mauritania.botinobe

package object models {

	type RecordId = Long

	type DeviceName = String
	type ActorName = String

	type PropName = String
	type PropValue = String

	type PropsMap = Map[PropName, PropValue]
	type ActorPropsMap = Map[ActorName, PropsMap]

	type Timestamp = Long

}
