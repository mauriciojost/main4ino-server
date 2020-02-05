package org.mauritania.main4ino.security.confgen

import org.mauritania.main4ino.security.MethodRight

object Actions {
  sealed trait Action

  case class AddRawUser(
    name: String,
    pass: String,
    email: String,
    granted: Map[String, MethodRight]
  ) extends Action

  case class AddRawUsers(
    users: List[AddRawUser]
  ) extends Action


}
