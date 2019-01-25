package org.mauritania.main4ino.config

import java.io.File

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import pureconfig.error.ConfigReaderException

import scala.reflect.ClassTag

trait Loadable {

  import pureconfig._

  def loadFromFile[T: ClassTag](configFile: File)(implicit reader: Derivation[ConfigReader[T]]): IO[T] = {
    IO {
      loadConfig[T](ConfigFactory.parseFile(configFile))
    }.flatMap {
      case Left(e) => {
        val absConfigFile = configFile.getAbsoluteFile
        val basicEx = new ConfigReaderException[T](e)
        val msgEx = new RuntimeException(s"Cannot parse configuration file $absConfigFile", basicEx)
        IO.raiseError[T](msgEx)
      }
      case Right(config) => IO.pure(config)
    }
  }

}
