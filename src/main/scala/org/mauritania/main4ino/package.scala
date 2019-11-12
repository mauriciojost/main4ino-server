package org.mauritania

import org.http4s.MediaType
import org.http4s.headers.`Content-Type`

package object main4ino {

  final val ContentTypeAppJson = `Content-Type`(MediaType.`application/json`)
  final val ContentTypeTextPlain = `Content-Type`(MediaType.`text/plain`)

}
