package org.mauritania.main4ino.models

import org.mauritania.main4ino.Fixtures
import org.scalatest.{Matchers, WordSpec}

class DeviceSpec extends WordSpec with Matchers {

  "The device" should {

    "keep the id and creation as reported by the DB" in {
      val d = Fixtures.DeviceId1
      d.id shouldBe d.dbId.id
      d.creation shouldBe d.dbId.creation
    }
  }

}
