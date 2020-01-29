package org.mauritania.main4ino.security

import org.http4s.Method
import org.http4s.Uri.Path

case class User(
  name: String,
  hashedpass: String,
  email: String,
  granted: Map[Path, MethodRight]
) {
  def authorized(method: Method, uriPath: Path): Option[User] = {
    granted.exists { case right => canAccess(right, method, uriPath)} match {
      case true => Some(this)
      case false => None
    }
  }
  val id = name

  private def canAccess(right: (Path, MethodRight), method: Method, uriPath: Path): Boolean = {
    val (uriGrant, methodGrant) = right
    methodGrant.canAccess(method) && uriPath.startsWith(uriGrant)
  }
}

