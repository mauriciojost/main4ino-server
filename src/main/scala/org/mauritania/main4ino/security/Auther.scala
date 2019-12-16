package org.mauritania.main4ino.security

import java.time.Clock

import cats.effect.Sync
import org.http4s.{BasicCredentials, Credentials, Headers, Request, Uri}
import org.http4s.Uri.Path
import org.http4s.util.CaseInsensitiveString
import org.mauritania.main4ino.security.Auther.{AccessAttempt, UserSession}
import org.reactormonk.CryptoBits
import com.github.t3hnar.bcrypt._
import org.http4s.headers.Authorization
import org.mauritania.main4ino.security.Config.UsersBy

import scala.util.Try

/**
  * Authorization and authentication
  * @tparam F
  */
class Auther[F[_]: Sync](config: Config) {

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
  def authenticateAndCheckAccessFromRequest(request: Request[F]): F[AccessAttempt] =
    Sync[F].delay(Auther.authenticateAndCheckAccess(config.usersBy, config.encryptionConfig, request.headers, request.uri, request.uri.path))

  /**
    * Provide a session given a user
    */
  def generateSession(user: User): F[UserSession] =
    Sync[F].delay(Auther.sessionFromUser(user, config.privateKeyBits, config.nonceStartupTime))
}

object Auther {

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
    * @param user user for whom we want to create a session
    * @param privateKey private key used for encryption of the session id to be generated
    * @param time time used to generate the session id
    * @return a user session id
    */
  def sessionFromUser(user: User, privateKey: CryptoBits, time: Clock): UserSession =
    privateKey.signToken(user.id, time.millis.toString())

  def authenticatedUserFromSessionOrCredentials(encry: EncryptionConfig, usersBy: UsersBy, session: Option[UserSession], creds: Option[(UserId, UserHashedPass)]): AuthenticationAttempt = {
    session.flatMap(v => encry.pkey.validateSignedToken(v)).flatMap(usersBy.byId.get)
      .orElse(creds.flatMap(usersBy.byIdPass.get))
      .toRight(s"Could not find related user (user:${creds.map(_._1)} / session:$session)")
  }

  /**
    * Check if a user can access a given resource
    */
  def checkAccess(user: User, resourceUriPath: Path): AccessAttempt = {
    user.authorized(dropTokenFromPath(resourceUriPath))
      .toRight(s"User '${user.name}' is not authorized to access resource '${resourceUriPath}'")
  }

  def userCredentialsFromRequest(encry: EncryptionConfig, headers: Headers, uri: Uri): Option[(UserId, UserHashedPass)] = {
    // Basic auth
    val credsFromHeader = headers.get(Authorization).collect {
      case Authorization(BasicCredentials(username, password)) => (username, hashPassword(password, encry.salt))
    }
    // URI auth: .../token/<token>/... authentication (some services like IFTTT do not support yet headers, only URI credentials...)
    val tokenFromUri = UriTokenRegex.findFirstMatchIn(uri.path).flatMap(a => Try(a.group(2)).toOption)
    val validCredsFromUri = tokenFromUri
      .map(t => BasicCredentials(t))
      .map(c => (c.username, hashPassword(c.password, encry.salt)))

    credsFromHeader
      .orElse(validCredsFromUri)
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
