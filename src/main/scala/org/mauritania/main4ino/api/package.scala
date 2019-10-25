package org.mauritania.main4ino

package object api {

  type ErrMsg = String

  type Attempt[T] = Either[ErrMsg, T]

}
