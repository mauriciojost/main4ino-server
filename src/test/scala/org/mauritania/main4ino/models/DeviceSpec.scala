package org.mauritania.main4ino.models

import org.mauritania.main4ino.Fixtures.Device1
import org.scalatest.{Matchers, WordSpec}

class DeviceSpec extends WordSpec with Matchers {

  "A device" should {

    "expands its properties" in {

      Device1.asActorTups.toSet shouldBe (
        Set(
          ActorTup(None, "dev1", "actorx", "xprop1", "xvalue1", Status.Created),
          ActorTup(None, "dev1", "actorx", "xprop2", "xvalue2", Status.Created),
          ActorTup(None, "dev1", "actorx", "xprop3", "xvalue3", Status.Created),

          ActorTup(None, "dev1", "actory", "yprop1", "yvalue1", Status.Created),
          ActorTup(None, "dev1", "actory", "yprop2", "yvalue2", Status.Created)
        )

        )
    }

  }

  "Device" should {

    "internal method should create a map from tuples" in {
      ActorMap.resolveFromTups(
        Iterable(
          ActorTup(Some(1L), "dev1", "actorx", "prop1", "1", Status.Created),
          ActorTup(Some(2L), "dev1", "actorx", "prop1", "2", Status.Created), // an update of the above
          ActorTup(Some(1L), "dev1", "actorx", "prop2", "1", Status.Created),
          ActorTup(Some(2L), "dev1", "actorx", "prop2", "1", Status.Created) // repeated with the above
        )
      ) shouldBe Map(
        "actorx" -> Map(
          "prop1" -> ("2", Status.Created),
          "prop2" -> ("1", Status.Created)
        )

      )
    }

    "internal method should pick first actor-prop if provided multiple times in same request ID" in {
      ActorMap.resolveFromTups(
        Iterable(
          ActorTup(Some(1L), "dev1", "actorx", "prop1", "1", Status.Created),
          ActorTup(Some(1L), "dev1", "actorx", "prop1", "2", Status.Created), // an override of the above (to be ignored)
        )
      ) shouldBe Map(
        "actorx" -> Map("prop1" -> ("1", Status.Created))
      )
    }

  }
}
