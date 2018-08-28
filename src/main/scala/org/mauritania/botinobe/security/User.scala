package org.mauritania.botinobe.security

case class User(
  id: Long,
  name: String,
  email: String,
  permissionPatterns: List[String],
  token: String
) {
  def allowed(url: String): Option[User] = {
    canAccess(url) match {
      case true => Some(this)
      case false => None
    }
  }
  def canAccess(url: String): Boolean = permissionPatterns.exists(p => url.startsWith(p))
}

