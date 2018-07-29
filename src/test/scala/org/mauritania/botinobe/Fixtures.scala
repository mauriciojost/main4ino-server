package org.mauritania.botinobe

import org.mauritania.botinobe.models.{Status, Device}
import org.mauritania.botinobe.models.Device.Metadata

object Fixtures {

  val Device1 = Device(
    Metadata(None, Status.Created, "dev1", Some(0L)),
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


}
