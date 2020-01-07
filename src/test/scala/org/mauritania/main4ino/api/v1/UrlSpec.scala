package org.mauritania.main4ino.api.v1

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UrlSpec extends AnyWordSpec with Matchers {

  "The extracting of safe strings from url" should {

    // TODO property testing better?

    "accept a numeric id" in {
      Url.extractSafeStringFrom("1001") should be(Some("1001"))
    }

    "accept an alphanumeric id" in {
      Url.extractSafeStringFrom("hey1001") should be(Some("hey1001"))
    }

    "accept an alphanumeric id with underscore" in {
      Url.extractSafeStringFrom("hey_1001") should be(Some("hey_1001"))
    }

    "reject an id with dots, slashes, dashes" in {
      Url.extractSafeStringFrom("hey.11") should be(None)
      Url.extractSafeStringFrom("hey/11") should be(None)
      Url.extractSafeStringFrom("hey-11") should be(None)
      Url.extractSafeStringFrom("../../etc/") should be(None)
    }

  }

}
