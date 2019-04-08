package org.mauritania.main4ino.models

import org.mauritania.main4ino.Fixtures.Device1
import org.scalatest.{Matchers, WordSpec}

class DeviceSpec extends WordSpec with Matchers {

  "A device" should {

    "expands its properties" in {

      Device1.asActorTups.toSet shouldBe (
        Set(
          ActorTup(None, "actorx", "xprop1", "xvalue1", None),
          ActorTup(None, "actorx", "xprop2", "xvalue2", None),
          ActorTup(None, "actorx", "xprop3", "xvalue3", None),

          ActorTup(None, "actory", "yprop1", "yvalue1", None),
          ActorTup(None, "actory", "yprop2", "yvalue2", None)
        )

        )
    }

  }

  "Device" should {

    "internal method should create a map from tuples" in {
      ActorMap.resolveFromTups(
        Iterable(
          ActorTup(Some(1L), "actorx", "prop1", "1", None),
          ActorTup(Some(2L), "actorx", "prop1", "2", None), // an update of the above
          ActorTup(Some(1L), "actorx", "prop2", "1", None),
          ActorTup(Some(2L), "actorx", "prop2", "1", None) // repeated with the above
        )
      ) shouldBe Map(
        "actorx" -> Map(
          "prop1" -> "2",
          "prop2" -> "1"
        )

      )
    }

    "internal method should pick first actor-prop if provided multiple times in same request ID" in {
      ActorMap.resolveFromTups(
        Iterable(
          ActorTup(Some(1L), "actorx", "prop1", "1", None),
          ActorTup(Some(1L), "actorx", "prop1", "2", None), // an override of the above (to be ignored)
        )
      ) shouldBe Map(
        "actorx" -> Map("prop1" -> "1")
      )
    }

  }
}
