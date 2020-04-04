package org.mauritania.main4ino.models

import org.mauritania.main4ino.Fixtures
import org.mauritania.main4ino.Fixtures.Device1
import org.mauritania.main4ino.db.Repository.{ActorTup, ActorTupIdLess}
import org.scalatest.ParallelTestExecution
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DeviceSpec extends AnyWordSpec with Matchers with ParallelTestExecution {

  "The device" should {

    "keep the id and creation as reported by the DB" in {
      val d = Fixtures.DeviceId1
      d.id shouldBe d.dbId.id
      d.creation shouldBe d.dbId.creation
    }
  }

  it should {

    "expand its properties" in {

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

  it should {

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
