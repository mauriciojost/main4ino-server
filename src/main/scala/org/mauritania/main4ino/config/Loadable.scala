package org.mauritania.main4ino.config

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import pureconfig.error.ConfigReaderException

import scala.reflect.ClassTag

trait Loadable {
  import pureconfig._

  def loadFromFile[T: ClassTag](configFile: String)(implicit reader: Derivation[ConfigReader[T]]): IO[T] = {
    IO {
      loadConfig[T](ConfigFactory.load(configFile))
    }.flatMap {
      case Left(e) => IO.raiseError[T](new ConfigReaderException[T](e))
      case Right(config) => IO.pure(config)
    }
  }

}
