package org.mauritania.main4ino.helpers

import java.io.File

import cats.effect.Sync
import com.typesafe.config.ConfigFactory
import pureconfig.error.ConfigReaderException

import scala.reflect.ClassTag

object ConfigLoader {

  import pureconfig._

  /**
    * Load a configuration from a file
    *
    * Ensure to have these imports before using:
    *   import pureconfig._
    *   import pureconfig.generic.auto._
    */
  def loadFromFile[F[_] : Sync, T: ClassTag](configFile: File)(implicit reader: Derivation[ConfigReader[T]]): F[T] = {
    val f = configFile.getAbsoluteFile
    implicitly[Sync[F]].fromEither {
      loadConfig[T](ConfigFactory.parseFile(f))
        .swap
        .map(i => new IllegalArgumentException(s"Cannot find/parse file '$f'", new ConfigReaderException[T](i)))
        .swap
    }
  }

}
