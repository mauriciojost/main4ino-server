package org.mauritania.main4ino.cli

object Data {
  case class RawUser(
    name: String,
    pass: String,
    email: String,
    granted: List[String]
  )


}
