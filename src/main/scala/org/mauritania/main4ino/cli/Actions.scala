package org.mauritania.main4ino.cli

object Actions {
  sealed trait CliAction

  case class AddRawUser(
    name: String,
    pass: String,
    email: String,
    granted: List[String]
  ) extends CliAction

}
