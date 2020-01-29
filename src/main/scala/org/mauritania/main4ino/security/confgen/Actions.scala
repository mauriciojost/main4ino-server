package org.mauritania.main4ino.security.confgen

import org.mauritania.main4ino.security.MethodRight

object Actions {
  sealed trait CliAction

  case class AddRawUser(
    name: String,
    pass: String,
    email: String,
    granted: Map[String, MethodRight]
  ) extends CliAction

  case class AddRawUsers(
    users: List[AddRawUser]
  ) extends CliAction


}
