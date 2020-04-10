package org.mauritania.main4ino.security

import org.http4s.Method
import enumeratum._

sealed trait Permission extends EnumEntry {
  def methodsAllowed: List[Method]
  def isAllowed(method: Method): Boolean = methodsAllowed.contains(method)
}

object Permission extends Enum[Permission] {

  val values = findValues

  case object R extends Permission {
    def methodsAllowed: List[Method] = List(Method.GET)
  }
  case object W extends Permission {
    def methodsAllowed: List[Method] = List(Method.PUT, Method.POST, Method.DELETE)
  }
  case object RW extends Permission {
    def methodsAllowed: List[Method] = List(Method.GET, Method.POST, Method.DELETE, Method.PUT)
  }
  case object - extends Permission {
    def methodsAllowed: List[Method] = List.empty[Method]
  }

}
