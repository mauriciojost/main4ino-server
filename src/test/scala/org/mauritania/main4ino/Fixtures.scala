package org.mauritania.main4ino

import org.mauritania.main4ino.models.{Device, DeviceId}
import org.mauritania.main4ino.models.Device.{DbId, Metadata}
import org.mauritania.main4ino.security.{Fixtures => FixturesSecurity}

object Fixtures {

  val DbId1 = DbId(1, 0L)
  val Device1 = Device(
    Metadata("dev1", Metadata.Status.Closed),
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

  val DeviceId1 = DeviceId(DbId1, Device1)

  val User1 = FixturesSecurity.User1
  val User1Pass = FixturesSecurity.User1Pass
  val Salt = FixturesSecurity.Salt

}
