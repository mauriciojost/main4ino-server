package org.mauritania.botinobe.security

object Authentication {

  val TokenKeyword = "token"
  val UniqueValidToken = "1122334455" // TODO: fix this hardcoded token

  case class User(id: Long, name: String)

  def toUser(id: Long): User = User(id, "Batman")

  def validate(token: String): Option[String] = {
    token match {
      case v if v == TokenKeyword + " " + UniqueValidToken => Some(UniqueValidToken)
      case _ => None
    }
  }

}
