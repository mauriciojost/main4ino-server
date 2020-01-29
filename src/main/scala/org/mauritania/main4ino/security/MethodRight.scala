package org.mauritania.main4ino.security

import org.http4s.Method

object MethodRight {
  sealed abstract class MethodRight(allowed: List[Method]) {
    def canAccess(method: Method): Boolean = allowed.contains(method)
  }

  case object R extends MethodRight(List(Method.GET))
  case object W extends MethodRight(List(Method.PUT, Method.POST, Method.DELETE))
  case object RW extends MethodRight(List(Method.GET, Method.POST, Method.DELETE, Method.PUT))
  case object - extends MethodRight(List.empty[Method])

  val All = List(R, W, RW, -)
  def parse(s: String): Option[MethodRight] = All.find(i => i.toString.equalsIgnoreCase(s))
}
