package org.mauritania.main4ino.helpers

import java.io.File

import cats.effect.Sync
import org.mauritania.main4ino.security.MethodRight
import pureconfig.error.ConfigReaderException
import pureconfig.generic.ProductHint

import scala.reflect.ClassTag

object ConfigLoader {

  import pureconfig._

  implicit val customReader = ConfigReader[Map[String, String]].map(m => m.flatMap{case (k, v) => MethodRight.parse(v).map((k, _))})


  /**
    * Load a configuration from a file
    *
    * Ensure to have these imports before using:
    *   import pureconfig._
    *   import pureconfig.generic.auto._
    */
  def loadFromFile[F[_] : Sync, T: ClassTag](configFile: File)(implicit reader: Derivation[ConfigReader[T]]): F[T] = {
    val f = configFile.getAbsoluteFile
    implicitly[Sync[F]].fromEither(ConfigSource.file(f).load.swap.map(ConfigReaderException(_)).swap)
  }

}
