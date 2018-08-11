package org.mauritania.botinobe.security

case class User(
  id: Long,
  name: String,
  email: String,
  permissionPatterns: List[String],
  token: String
) {
  def canAccess(url: String): Option[User] = {
    permissionPatterns.exists(p => url.startsWith(p)) match {
      case true => Some(this)
      case false => None
    }
  }
}

