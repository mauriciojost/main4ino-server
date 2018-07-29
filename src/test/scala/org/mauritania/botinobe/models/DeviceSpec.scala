package org.mauritania.botinobe.models

import org.mauritania.botinobe.Fixtures.Device1
import org.mauritania.botinobe.models.Device.Metadata
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

    "merge two similar targets" in {
      val t1 = Device1
      val t2 = Device1

      val merged = Device.merge(Seq(t1, t2))

      merged.size shouldBe (1)
      merged(0).metadata shouldBe (Device1.withStatus(Status.Merged).withTimestamp(None).metadata)
      merged(0).actors("actorx").toSet shouldBe (Device1.actors("actorx").toSet)
      merged(0).actors("actory").toSet shouldBe (Device1.actors("actory").toSet)
    }

    "merge targets (take last xprop1 and add new xprop2)" in {
      val t1 = Device(Metadata(Some(1L), None, "dev1"), // first target request
        Map("actorx" -> Map("xprop1" -> ("xvalue1", Status.Created)))
      )
      val t2 = Device(Metadata(Some(2L), None, "dev1"), // second target request
        Map("actorx" -> Map("xprop1" -> ("xvalueU", Status.Created), "xprop2" -> ("xvalue2", Status.Created)))
      )

      val merged = Device.merge(Seq(t1, t2))

      merged.size shouldBe 1
      merged(0).metadata shouldBe Metadata(None, None, "dev1")
      merged(0).actors.keys.toSet shouldBe Set("actorx")
      merged(0).actors("actorx").toSet shouldBe Set(
        "xprop1" -> ("xvalueU", Status.Created),
        "xprop2" -> ("xvalue2", Status.Created)
      )

    }


    "internal method should create a map from tuples" in {
      ActorTup.asActorMap(
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

  }
}
