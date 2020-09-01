package org.mauritania.main4ino.security.confgen

import org.mauritania.main4ino.models.DeviceName
import org.mauritania.main4ino.security.Permission
import org.mauritania.main4ino.security.confgen.Actions.{Action, AddRawUsers}

case class Actions(
  addUsers: Option[AddRawUsers]
) {
  def merged(): List[Action] = addUsers.toList
}

object Actions {

  case class AddRawUser(
    name: String,
    pass: String,
    email: String,
    granted: Map[String, Permission],
    devices: List[DeviceName]
  )

  sealed trait Action
  case object Identity extends Action
  case class AddRawUsers(users: List[AddRawUser]) extends Action

}
