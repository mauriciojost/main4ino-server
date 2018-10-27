package org.mauritania.main4ino.security

import org.http4s.Uri.Path

case class User(
  id: Long,
  name: String,
  email: String,
  permissionPatterns: List[String],
  token: String
) {
  def authorized(uriPath: Path): Option[User] = {
    canAccess(uriPath) match {
      case true => Some(this)
      case false => None
    }
  }
  private [security] def canAccess(uriPath: Path): Boolean = permissionPatterns.exists(p => uriPath.startsWith(p))
}

