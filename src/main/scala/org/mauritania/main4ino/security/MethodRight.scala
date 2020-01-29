package org.mauritania.main4ino.security

import org.http4s.Method
import enumeratum._

sealed trait MethodRight extends EnumEntry {
  def allowed: List[Method]
  def canAccess(method: Method): Boolean = allowed.contains(method)
}

object MethodRight extends Enum[MethodRight] {

  val values = findValues

  case object R extends MethodRight {
    def allowed = List(Method.GET)
  }
  case object W extends MethodRight {
    def allowed = List(Method.PUT, Method.POST, Method.DELETE)
  }
  case object RW extends MethodRight {
    def allowed = List(Method.GET, Method.POST, Method.DELETE, Method.PUT)
  }
  case object - extends MethodRight {
    def allowed = List.empty[Method]
  }

}
