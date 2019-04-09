package org.mauritania.main4ino

import org.mauritania.main4ino.Fixtures.Device1
import org.mauritania.main4ino.Repository.{ActorTup, ActorTupIdLess}
import org.scalatest.{Matchers, WordSpec}

class RepositorySpec extends WordSpec with Matchers {

  "A device" should {

    "expands its properties" in {

      Device1.asActorTups.toSet shouldBe (
        Set(
          ActorTupIdLess("actorx", "xprop1", "xvalue1"),
          ActorTupIdLess("actorx", "xprop2", "xvalue2"),
          ActorTupIdLess("actorx", "xprop3", "xvalue3"),

          ActorTupIdLess("actory", "yprop1", "yvalue1"),
          ActorTupIdLess("actory", "yprop2", "yvalue2")
        )

        )
    }

  }

  "Device" should {

    "internal method should create a map from tuples" in {
      ActorTup.asActorMap(
        Iterable(
          ActorTup(1L, "actorx", "prop1", "1", 0L),
          ActorTup(2L, "actorx", "prop1", "2", 0L), // an update of the above
          ActorTup(1L, "actorx", "prop2", "1", 0L),
          ActorTup(2L, "actorx", "prop2", "1", 0L) // repeated with the above
        )
      ) shouldBe Map(
        "actorx" -> Map(
          "prop1" -> "2",
          "prop2" -> "1"
        )

      )
    }

    "internal method should pick last actor-prop if provided multiple times in same request ID" in {
      ActorTup.asActorMap(
        Iterable(
          ActorTup(1L, "actorx", "prop1", "1", 0L),
          ActorTup(1L, "actorx", "prop1", "2", 0L), // an override of the above (to be ignored)
        )
      ) shouldBe Map(
        "actorx" -> Map("prop1" -> "2")
      )
    }

  }

}
