package org.mauritania.main4ino.api.v1

import io.circe.{Decoder, parser}
import io.circe.generic.auto._
import org.scalatest.{Matchers, WordSpec}

class JsonEncodingSpec extends WordSpec with Matchers {

  case class B(i: Boolean) // object containing a boolean
  case class I(i: Int) // object containing an integer
  case class S(i: String) // object containing a string
  case class A(i: Array[String]) // object containing an array of strings

  "Default json string decoder" should {

    "decode a string" in {
      parser.parse("""{"i": "string"}""").flatMap(Decoder[S].decodeJson) shouldBe Right(S("string"))
    }
    "decode an integer" in {
      parser.parse("""{"i": 1}""").flatMap(Decoder[I].decodeJson) shouldBe Right(I(1))
    }
    "decode a boolean" in {
      parser.parse("""{"i": true}""").flatMap(Decoder[B].decodeJson) shouldBe Right(B(true))
    }

    "decode an array" in {
      parser.parse("""{"i": []}""").flatMap(Decoder[A].decodeJson).isRight shouldBe true
    }

  }

  "Custom json string decoder" should {

    implicit val decCustom = JsonEncoding.StringDecoder // as provided by this project

    "decode a string into a string" in {
      parser.parse("""{"i": "string"}""").flatMap(Decoder[S].decodeJson) shouldBe Right(S("string"))
    }
    "decode an integer into a string" in {
      parser.parse("""{"i": 1}""").flatMap(Decoder[S].decodeJson) shouldBe Right(S("1"))
    }
    "decode a boolean into a string" in {
      parser.parse("""{"i": true}""").flatMap(Decoder[S].decodeJson) shouldBe Right(S("true"))
    }
    "fail to decode an array into a string" in {
      parser.parse("[]").flatMap(Decoder[S].decodeJson).isRight shouldBe false
    }
  }

}
