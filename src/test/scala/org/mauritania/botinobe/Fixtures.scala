package org.mauritania.botinobe

import org.mauritania.botinobe.models.{Status, Device}
import org.mauritania.botinobe.models.Device.Metadata

object Fixtures {

  val Device1 = Device(
    Metadata(Status.Created, "dev1", Some(0L)),
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
