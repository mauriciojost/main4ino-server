package org.mauritania.main4ino.helpers

import java.io.File

import cats.effect.Sync
import org.mauritania.main4ino.security.MethodRight
import pureconfig.backend.ConfigFactoryWrapper
import pureconfig.error.ConfigReaderException

import scala.reflect.ClassTag

object ConfigLoader {

  import pureconfig._
  import enumeratum._

  object CirceImplicits {
    implicit val methodRightEncoder = Circe.encoder(MethodRight)
    implicit val methodRightDecoder = Circe.decoder(MethodRight)
  }
  object PureConfigImplicits {
    implicit val customMethodRightReader = ConfigReader[String].map(m => MethodRight.withName(m))
  }




  /**
    * Load a configuration from a file
    *
    * Ensure to have these imports before using:
    *   import pureconfig._
    *   import pureconfig.generic.auto._
    */
  def fromFile[F[_] : Sync, T: ClassTag](configFile: File)(implicit reader: Derivation[ConfigReader[T]]): F[T] = {
    val f = configFile.getAbsoluteFile
    from(ConfigSource.file(f))
  }

  def fromFileAndEnv[F[_] : Sync, T: ClassTag](configFile: File)(implicit reader: Derivation[ConfigReader[T]]): F[T] = {
    val f = configFile.getAbsoluteFile
    from(ConfigSource.systemProperties.withFallback(ConfigSource.file(f)))
  }

  def fromEnv[F[_] : Sync, T: ClassTag](implicit reader: Derivation[ConfigReader[T]]): F[T] = {
    from(ConfigSource.systemProperties)
  }

  def from[F[_] : Sync, T: ClassTag](s: ConfigObjectSource)(implicit reader: Derivation[ConfigReader[T]]): F[T] = {
    implicitly[Sync[F]].fromEither(s.load.swap.map(ConfigReaderException(_)).swap)
  }

}
