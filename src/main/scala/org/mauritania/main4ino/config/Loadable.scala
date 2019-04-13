package org.mauritania.main4ino.config

import java.io.File

import cats.effect.Sync
import com.typesafe.config.ConfigFactory
import pureconfig.error.ConfigReaderException

import scala.reflect.ClassTag

object Loadable {

  import pureconfig._

  // TODO is there any interest in doing this a trait over an object?
  // How to implement this as an algebra+module with a clean API (only dependant on T, for any T, without a Derivation[...])
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
