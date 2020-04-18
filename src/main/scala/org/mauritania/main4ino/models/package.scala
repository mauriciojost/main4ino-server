package org.mauritania.main4ino

package object models {

  type RequestId = Long

  type DeviceName = String
  type ActorName = String

  // Properties name and value
  type PropName = String
  type PropValue = String

  // Properties of a given actor
  type ActorProps = Map[PropName, PropValue]

  // Properties of the actors of a device
  type DeviceProps = Map[ActorName, ActorProps]

  // Timestamp in seconds from the epoch (in UTC)
  type EpochSecTimestamp = Long

  // Project name (like botino, sleepino, etc.)
  type ProjectName = String

  // Id of the firmware
  type FirmwareVersion = String

  // Identifier of a specific version of the firmware (i.e. git commit), represented in some way
  type VersionWish = String

  // Embedded platform identifier (esp8266, esp32, etc.)
  type Platform = String

  // Feature name (i.e. git branch name, for development purposes)
  type Feature = String

  // File name of the firmware
  type FirmwareFilename = String

}
