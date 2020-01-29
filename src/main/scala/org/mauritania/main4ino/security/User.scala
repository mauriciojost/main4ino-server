package org.mauritania.main4ino.security

import org.http4s.Method
import org.http4s.Uri.Path
import org.mauritania.main4ino.security.MethodRight.MethodRight

case class User(
  name: String,
  hashedpass: String,
  email: String,
  granted: Map[String, MethodRight]
) {
  def authorized(method: Method, uriPath: Path): Option[User] = {
    granted.exists{case (k, v) => AccessRight(k, v).canAccess(method, uriPath)} match {
      case true => Some(this)
      case false => None
    }
  }
  val id = name
}

