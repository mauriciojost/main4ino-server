package org.mauritania.botinobe.models

import org.mauritania.botinobe.Fixtures.TargetFixture1
import org.mauritania.botinobe.models.Target.Metadata
import org.scalatest.{Matchers, WordSpec}

class TargetSpec extends WordSpec with Matchers {

  "A target" should {

    "expands its properties" in {

      TargetFixture1.expandProps.toSet shouldBe(
        Set(
          Prop("actorx", "xprop1", "xvalue1"),
          Prop("actorx", "xprop2", "xvalue2"),
          Prop("actorx", "xprop3", "xvalue3"),

          Prop("actory", "yprop1", "yvalue1"),
          Prop("actory", "yprop2", "yvalue2")
        )

      )
    }

  }

  "Targets" should {

    "merge two similar targets" in {
      val t1 = TargetFixture1
      val t2 = TargetFixture1

      val merged = Target.merge(t1.metadata.device, t2.metadata.status, Seq(t1, t2))

      merged.size shouldBe(1)
      merged(0).metadata shouldBe(TargetFixture1.metadata.copy(status = Status.Merged, timestamp = None))
      merged(0).props("actorx").toSet shouldBe(TargetFixture1.props("actorx").toSet)
      merged(0).props("actory").toSet shouldBe(TargetFixture1.props("actory").toSet)
    }

    "merge targets (take last xprop1 and add new xprop2)" in {
      val t1 = Target(Metadata(Status.Created, "dev1", Some(1L)), // first target request
        Map("actorx" -> Map("xprop1" -> "xvalue1"))
      )
      val t2 = Target(Metadata(Status.Created, "dev1", Some(2L)), // second target request
        Map("actorx" -> Map("xprop1" -> "xvalueU", "xprop2" -> "xvalue2"))
      )

      val merged = Target.merge("dev1", Status.Created, Seq(t1, t2))

      merged.size shouldBe(1)
      merged(0).metadata shouldBe(Metadata(Status.Merged, "dev1", None))
      merged(0).props.keys.toSet shouldBe(Set("actorx"))
      merged(0).props("actorx").toSet shouldBe(
        Set(
          "xprop1" -> "xvalueU",
          "xprop2" -> "xvalue2"
        )
      )
    }


    "internal method " in {
      Target.tuple4Seq2MapOfMaps[ActorName, PropName, Timestamp, PropValue](
        Seq(
          ("actorx", "prop1", 1L, "1"),
          ("actorx", "prop1", 2L, "2"), // an update of the above
          ("actorx", "prop2", 1L, "1"),
          ("actorx", "prop2", 1L, "1") // repeated with the above
        )
      ) shouldBe(
        Map(
          "actorx" -> Map(
            "prop1" -> Map(
              1L -> "1",
              2L -> "2"
            ),
            "prop2" -> Map(
              1L -> "1"
            )
          )
        )
      )
    }

  }
}
