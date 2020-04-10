package org.mauritania.main4ino.security

import org.http4s.Method
import org.http4s.Uri.Path
import org.mauritania.main4ino.security.Auther.UserHashedPass

case class User(
  name: String,
  hashedpass: UserHashedPass,
  email: String,
  granted: Map[Path, Permission]
) {
  def authorized(method: Method, uriPath: Path): Option[User] = {
    granted.exists { case right => canAccess(right, method, uriPath) } match {
      case true => Some(this)
      case false => None
    }
  }
  val id = name

  private def canAccess(right: (Path, Permission), method: Method, uriPath: Path): Boolean = {
    val (uriGrant, methodGrant) = right
    methodGrant.isAllowed(method) && uriPath.startsWith(uriGrant)
  }
}
