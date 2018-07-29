package org.mauritania.botinobe.models

import org.mauritania.botinobe.Fixtures.Device1
import org.mauritania.botinobe.models.Device.Metadata
import org.scalatest.{Matchers, WordSpec}

class DeviceSpec extends WordSpec with Matchers {

  "A device" should {

    "expands its properties" in {

      Device1.asActorTups.toSet shouldBe(
        Set(
          ActorTup("actorx", "xprop1", ("xvalue1", Status.Created)),
          ActorTup("actorx", "xprop2", ("xvalue2", Status.Created)),
          ActorTup("actorx", "xprop3", ("xvalue3", Status.Created)),

          ActorTup("actory", "yprop1", ("yvalue1", Status.Created)),
          ActorTup("actory", "yprop2", ("yvalue2", Status.Created))
        )

      )
    }

  }

  "Device" should {

    "merge two similar targets" in {
      val t1 = Device1
      val t2 = Device1

      val merged = Device.merge(t1.metadata.device, t2.metadata.status, Seq(t1, t2))

      merged.size shouldBe(1)
      merged(0).metadata shouldBe(Device1.withStatus(Status.Merged).withTimestamp(None).metadata)
      merged(0).actors("actorx").toSet shouldBe(Device1.actors("actorx").toSet)
      merged(0).actors("actory").toSet shouldBe(Device1.actors("actory").toSet)
    }

    "merge targets (take last xprop1 and add new xprop2)" in {
      val t1 = Device(Metadata(None, Status.Created, "dev1", Some(1L)), // first target request
        Map("actorx" -> Map("xprop1" -> ("xvalue1", Status.Created)))
      )
      val t2 = Device(Metadata(None, Status.Created, "dev1", Some(2L)), // second target request
        Map("actorx" -> Map("xprop1" -> ("xvalueU", Status.Created), "xprop2" -> ("xvalue2", Status.Created)))
      )

      val merged = Device.merge("dev1", Status.Created, Seq(t1, t2))

      merged.size shouldBe(1)
      merged(0).metadata shouldBe(Metadata(None, Status.Merged, "dev1", None))
      merged(0).actors.keys.toSet shouldBe(Set("actorx"))
      merged(0).actors("actorx").toSet shouldBe(
        Set(
          "xprop1" -> ("xvalueU", Status.Created),
          "xprop2" -> ("xvalue2", Status.Created)
        )
      )
    }


    "internal method " in {
      Device.tuple4Seq2MapOfMaps[ActorName, PropName, Timestamp, PropValueStatus](
        Seq(
          ("actorx", "prop1", 1L, ("1", Status.Created)),
          ("actorx", "prop1", 2L, ("2", Status.Created)), // an update of the above
          ("actorx", "prop2", 1L, ("1", Status.Created)),
          ("actorx", "prop2", 1L, ("1", Status.Created)) // repeated with the above
        )
      ) shouldBe(
        Map(
          "actorx" -> Map(
            "prop1" -> Map(
              1L -> ("1", Status.Created),
              2L -> ("2", Status.Created)
            ),
            "prop2" -> Map(
              1L -> ("1", Status.Created)
            )
          )
        )
      )
    }

  }
}
