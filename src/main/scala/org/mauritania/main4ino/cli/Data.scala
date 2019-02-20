package org.mauritania.main4ino.cli

object Data {
  case class AddRawUserParams(
    name: String,
    pass: String,
    email: String,
    granted: List[String]
  )
}
