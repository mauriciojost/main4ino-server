package org.mauritania.botinobe

import org.mauritania.botinobe.api.v1.DeviceU
import org.mauritania.botinobe.api.v1.DeviceU.MetadataU
import org.mauritania.botinobe.models.{Device, Status}
import org.mauritania.botinobe.models.Device.Metadata

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

  val Device1InV1 = DeviceU(
    MetadataU(None, Some(0L), "dev1"),
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






}
