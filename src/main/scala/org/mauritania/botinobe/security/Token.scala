package org.mauritania.botinobe.security

object Token {

  val TokenKeyword = "token"
  val UniqueValidToken = "1122334455" // TODO: fix this hardcoded token

  case class User(id: Long, name: String)

  def toUser(id: Long): User = User(id, "Batman")

  def validate(value: String): Option[String] = {
    value match {
      case v if v == TokenKeyword + " " + UniqueValidToken => Some(UniqueValidToken)
      case _ => None
    }
  }

}
