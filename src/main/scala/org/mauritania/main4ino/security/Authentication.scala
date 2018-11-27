package org.mauritania.main4ino.security

import java.time.Clock

import cats.effect.IO
import org.http4s.{BasicCredentials, Headers, Request, Uri}
import org.http4s.Uri.Path
import org.http4s.util.CaseInsensitiveString
import org.mauritania.main4ino.security.Authentication.{AccessAttempt, UserSession}
import org.reactormonk.CryptoBits
import com.github.t3hnar.bcrypt._
import org.http4s.headers.Authorization
import org.mauritania.main4ino.security.Config.UsersBy

import scala.util.Try

trait Authentication[F[_]] { // TODO Rename as Auther (authentication & authorization)

  /**
    * Authenticate the user given a request and attempt an access to the resource
    *
    * Implemented as a Kleisli where the request is given, and a effect is
    * obtained as a result, containing the result of the authentication as a
    * [[AccessAttempt]].
    * It should interpret:
    * - session from headers
    * - user/pass from headers
    * - user/pass from uri
    *
    * The resource to be accessed is in the uri of the request.
    */
  def authenticateAndCheckAccessFromRequest(request: Request[F]): F[AccessAttempt]

  /**
    * Provide a session given a user
    */
  def generateSession(user: User): F[UserSession]

}

class AuthenticationIO(config: Config) extends Authentication[IO] {

  def authenticateAndCheckAccessFromRequest(request: Request[IO]): IO[AccessAttempt] =
    IO.pure(Authentication.authenticateAndCheckAccess(config.usersBy, config.encryptionConfig, request.headers, request.uri, request.uri.path))

  def generateSession(user: User): IO[UserSession] =
    IO.pure(Authentication.sessionFromUser(user, config.privateKeyBits, config.nonceStartupTime))
}

object Authentication {

  type UserId = String // username
  type UserHashedPass = String // hashed password
  type UserSession = String // generated after login

  type ErrorMsg = String
  type AccessAttempt = Either[ErrorMsg, User]
  type AuthenticationAttempt = Either[ErrorMsg, User]

  private final val HeaderSession = CaseInsensitiveString("session")
  private final val UriTokenRegex = ("^(.*)/token/(.*?)/(.*)$").r

  def authenticateAndCheckAccess(usersBy: UsersBy, encry: EncryptionConfig, headers: Headers, uri: Uri, resource: Path): AccessAttempt = {
    val credentials = userCredentialsFromRequest(encry, headers, uri)
    val session = sessionFromRequest(headers)
    for {
      user <- authenticatedUserFromSessionOrCredentials(encry, usersBy, session, credentials)
      authorized <- checkAccess(user, resource)
    } yield authorized
  }

  /**
    * Create a session for a given user given a private key and a timestamp
    *
    * @param user
    * @param privateKey
    * @param time
    * @return
    */
  def sessionFromUser(user: User, privateKey: CryptoBits, time: Clock): UserSession =
    privateKey.signToken(user.id, time.millis.toString())

  def authenticatedUserFromSessionOrCredentials(encry: EncryptionConfig, usersBy: UsersBy, session: Option[UserSession], creds: Option[(UserId, UserHashedPass)]): AuthenticationAttempt = {
    creds.flatMap(usersBy.byIdPass.get)
      .orElse(session.flatMap(v => encry.pkey.validateSignedToken(v)).flatMap(usersBy.byId.get))
      .toRight(s"Could not find user for session $session or credentials $creds")
  }

  def checkAccess(user: User, resourceUriPath: Path): AccessAttempt = {
    user.authorized(dropTokenFromPath(resourceUriPath))
      .toRight(s"User ${user.name} is not authorized to access resource ${resourceUriPath}")
  }

  def userCredentialsFromRequest(encry: EncryptionConfig, headers: Headers, uri: Uri): Option[(UserId, UserHashedPass)] = {
    // Basic auth
    val credsFromHeader = headers.get(Authorization).collect {
      case Authorization(BasicCredentials(token)) => (token.username, hashPassword(token.password, encry.salt))
    }
    // URI auth: .../token/<token>/... authentication (some services like IFTTT do not support yet headers, only URI credentials...)
    val tokenFromUri = UriTokenRegex.findFirstMatchIn(uri.path).flatMap(a => Try(a.group(2)).toOption)
    val validCredsFromUri = tokenFromUri.map(t => BasicCredentials(t)).map(c => (c.username, hashPassword(c.password, encry.salt)))

    credsFromHeader.orElse(validCredsFromUri)
  }

  def sessionFromRequest(headers: Headers): Option[UserSession] = headers.get(HeaderSession).map(_.value)

  def hashPassword(password: String, salt: String): String = password.bcrypt(salt)

  private def dropTokenFromPath(path: Path): Path = {
    UriTokenRegex.findFirstMatchIn(path).map(m => m.group(1) + "/" + m.group(3)).getOrElse(path)
  }

  /**
    * Encryption configuration
    *
    * @param pkey private key used to generate sessions
    * @param salt salt used to hash passwords
    */
  case class EncryptionConfig(
    pkey: CryptoBits,
    salt: String
  )

}
