package org.mauritania.botinobe

import org.mauritania.botinobe.models.Target
import org.mauritania.botinobe.models.Target.Metadata

object Fixtures {

  val TargetFixture1 = Target(
    Metadata(Target.Created, "dev1", Some(0L)),
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
