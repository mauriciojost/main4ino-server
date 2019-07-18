package org.mauritania.main4ino.models

import io.circe.Json
import org.mauritania.main4ino.models.Description.VersionJson

/**
  * Description of a device: its actors and their properties, examples and explanations
  *
  * This class allows the devices to describe their actors and properties as their firmware evolves,
  * decoupling the update in the FE.
  *
  */
case class Description(
  device: DeviceName,
  updated: EpochSecTimestamp,
  versionJson: VersionJson
)

object Description {
  case class VersionJson(
    version: String,
    json: Json
  )
}