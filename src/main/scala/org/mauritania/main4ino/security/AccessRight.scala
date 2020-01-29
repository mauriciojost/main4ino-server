package org.mauritania.main4ino.security

import org.http4s.Method
import org.http4s.Uri.Path
import org.mauritania.main4ino.security.MethodRight.MethodRight

case class AccessRight(
  allowedUriPrefix: String,
  allowedMethod: MethodRight
) {

  def canAccess(method: Method, uriPath: Path): Boolean = {
    allowedMethod.canAccess(method) && uriPath.startsWith(allowedUriPrefix)
  }
}
