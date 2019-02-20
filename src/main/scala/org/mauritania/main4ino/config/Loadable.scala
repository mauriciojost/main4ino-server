package org.mauritania.main4ino.config

import java.io.File

import cats.effect.Sync
import com.typesafe.config.ConfigFactory
import pureconfig.error.ConfigReaderException

import scala.reflect.ClassTag

object Loadable {

  import pureconfig._

  def loadFromFile[F[_] : Sync, T: ClassTag](configFile: File)(implicit reader: Derivation[ConfigReader[T]]): F[T] = {
    implicitly[Sync[F]].fromEither {
      loadConfig[T](ConfigFactory.parseFile(configFile))
        .swap
        .map(i => new IllegalArgumentException(s"Cannot parse: $configFile", new ConfigReaderException[T](i)))
        .swap
    }
  }

}
