package org.mauritania.main4ino

import org.mauritania.main4ino.api.v1.DeviceV1
import org.mauritania.main4ino.api.v1.DeviceV1.MetadataV1
import org.mauritania.main4ino.models.{Device, Status}
import org.mauritania.main4ino.models.Device.Metadata

import org.mauritania.main4ino.security.{Fixtures => FixturesSecurity}

object Fixtures {

  val Device1 = Device(
    Metadata(None, Some(0L), "dev1"),
    Map(
      "actorx" ->
        Map(
          "xprop1" -> ("xvalue1", Status.Created),
          "xprop2" -> ("xvalue2", Status.Created),
          "xprop3" -> ("xvalue3", Status.Created)
        ),
      "actory" ->
        Map(
          "yprop1" -> ("yvalue1", Status.Created),
          "yprop2" -> ("yvalue2", Status.Created)
        )
    )
  )

  val Device1InV1 = DeviceV1(
    MetadataV1(None, Some(0L), "dev1"),
    Map(
      "actorx" ->
        Map(
          "xprop1" -> "xvalue1",
          "xprop2" -> "xvalue2",
          "xprop3" -> "xvalue3"
        ),
      "actory" ->
        Map(
          "yprop1" -> "yvalue1",
          "yprop2" -> "yvalue2"
        )
    )
  )

  val User1 = FixturesSecurity.User1
  val User1Pass = FixturesSecurity.User1Pass
  val Salt = FixturesSecurity.Salt

}
