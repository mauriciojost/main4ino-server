package org.mauritania.main4ino.helpers

import java.io.File

import cats.effect.Sync
import io.circe.{Decoder, Encoder}
import org.mauritania.main4ino.security.Auther.UserHashedPass
import org.mauritania.main4ino.security.Permission
import pureconfig.error.{CannotParse, ConfigReaderException, ConfigReaderFailures}
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import scala.reflect.ClassTag
import scala.util.Try

object ConfigLoader {

  import enumeratum._
  import pureconfig._

  object CirceImplicits {
    implicit val permissionEncoder: Encoder[Permission] = Circe.encoder(Permission)
    implicit val permissionDecoder: Decoder[Permission] = Circe.decoder(Permission)
    implicit val methodUserHashedPassDecoder: Decoder[UserHashedPass] =
      implicitly[Decoder[String]].map(s => PasswordHash[BCrypt](s))
    implicit val methodUserHashedPassEncoder: Encoder[UserHashedPass] =
      implicitly[Encoder[String]].contramap[UserHashedPass](m => m.toString)
    implicit val byteArrayEncoder: Encoder[Array[Byte]] =
      implicitly[Encoder[String]].contramap[Array[Byte]](m => m.map("%02X" format _).mkString)
  }

  object PureConfigImplicits {

    implicit object customPermissionReader extends ConfigReader[Permission] {
      def from(cur: ConfigCursor): Either[ConfigReaderFailures, Permission] = {
        val st = cur.asString
        st.flatMap { s =>
          Permission.withNameEither(s).left.map { e =>
            ConfigReaderFailures(CannotParse(e.getMessage(), None))
          }
        }
      }
    }

    implicit object customByteArray extends ConfigReader[Array[Byte]] {
      def from(cur: ConfigCursor): Either[ConfigReaderFailures, Array[Byte]] = {
        val st = cur.asString
        st.flatMap { s =>
          Try(s.grouped(2).toArray map { a =>
            val parsed = Integer.parseInt(a, 16).toByte
            parsed
          }).toEither.left.map { e =>
            ConfigReaderFailures(CannotParse(e.getMessage(), None))
          }
        }
      }
    }

    implicit val customMethodUserHashedPass: ConfigReader[UserHashedPass] =
      ConfigReader[String].map(m => PasswordHash[BCrypt](m))
  }

  /**
    * Load a configuration from a file
    *
    * Ensure to have these imports before using:
    * import pureconfig._
    * import pureconfig.generic.auto._
    */
  def fromFile[F[_] : Sync, T: ClassTag](
    configFile: File
  )(implicit reader: Derivation[ConfigReader[T]]): F[T] = {
    val f = configFile.getAbsoluteFile
    from(ConfigSource.file(f))
  }

  def fromFileAndEnv[F[_] : Sync, T: ClassTag](
    configFile: File
  )(implicit reader: Derivation[ConfigReader[T]]): F[T] = {
    val f = configFile.getAbsoluteFile
    from(ConfigSource.systemProperties.withFallback(ConfigSource.file(f)))
  }

  def fromEnv[F[_] : Sync, T: ClassTag](implicit reader: Derivation[ConfigReader[T]]): F[T] = {
    from(ConfigSource.systemProperties)
  }

  def from[F[_] : Sync, T: ClassTag](
    s: ConfigObjectSource
  )(implicit reader: Derivation[ConfigReader[T]]): F[T] = {
    implicitly[Sync[F]].fromEither(s.load.swap.map(ConfigReaderException(_)).swap)
  }

}
