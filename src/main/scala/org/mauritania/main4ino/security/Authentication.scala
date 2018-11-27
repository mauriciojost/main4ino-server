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
  def authUserFromRequest(request: Request[F]): F[AccessAttempt]

  /**
    * Provide a session given a user
    */
  def generateUserSession(user: User): F[UserSession]

}

class AuthenticationIO(config: Config) extends Authentication[IO] {

  def authUserFromRequest(request: Request[IO]): IO[AccessAttempt] =
    IO.pure(Authentication.authenticateUser(config.usersBy, config.encryptionConfig, request.headers, request.uri, request.uri.path))

  def generateUserSession(user: User): IO[UserSession] =
    IO.pure(Authentication.sessionFromUser(user, config.privateKeyBits, config.nonceStartupTime))
}

object Authentication {

  type UserId = String // username
  type UserHashedPass = String // hashed password
  type UserSession = String // generated after login

  type AccessErrorMsg = String
  type AccessAttempt = Either[AccessErrorMsg, User]

  private final val HeaderSession = CaseInsensitiveString("session")
  private final val UriTokenRegex = ("^(.*)/token/(.*?)/(.*)$").r

  // TODO authenticated != authorized, review whole file to ensure naming is correct
  def authenticateUser(usersBy: UsersBy, encry: EncryptionConfig, headers: Headers, uri: Uri, resource: Path): AccessAttempt = {
    val userCredentials = userCredentialsFromCredentialsRequest(encry, headers, uri)
    val userIdSession = userIdFromValidSessionRequest(headers)
    val user = authenticatedUserFromSessionOrCredentials(encry, usersBy, userIdSession, userCredentials)
    val authorized = isAuthorizedToAccess(user, resource)
    authorized
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

  def authenticatedUserFromSessionOrCredentials(encry: EncryptionConfig, usersBy: UsersBy, userIdSession: Option[UserId], userIdPass: Option[(UserId, UserHashedPass)]): AccessAttempt = {
    userIdPass.flatMap(usersBy.byIdPass.get)
      .orElse(userIdSession.flatMap(v => encry.pkey.validateSignedToken(v)).flatMap(usersBy.byId.get))
      .toRight(s"Could not find user for session $userIdSession or userIdPass $userIdPass")
  }

  def isAuthorizedToAccess(user: AccessAttempt, resourceUriPath: Path): AccessAttempt = {
    for {
      u <- user
      ua <- u.authorized(discardToken(resourceUriPath))
        .toRight(s"User ${u.name} is not authorized to access resource ${resourceUriPath}")
    } yield (ua)
  }

  def userCredentialsFromCredentialsRequest(encry: EncryptionConfig, headers: Headers, uri: Uri): Option[(UserId, UserHashedPass)] = {
    // Basic auth
    val credsFromHeader = headers.get(Authorization).collect {
      case Authorization(BasicCredentials(token)) => (token.username, passHash(token.password, encry.salt))
    }
    // .../token/<token>/... authentication (some services like IFTTT do not support yet headers, only URI credentials...)
    val tokenFromUri = UriTokenRegex.findFirstMatchIn(uri.path).flatMap(a => Try(a.group(2)).toOption)
    val validCredsFromUri = tokenFromUri.map(t => BasicCredentials(t)).map(c => (c.username, passHash(c.password, encry.salt)))

    credsFromHeader.orElse(validCredsFromUri)
  }

  def userIdFromValidSessionRequest(headers: Headers): Option[UserSession] = headers.get(HeaderSession).map(_.value)

  def passHash(clearPass: String, salt: String): String = clearPass.bcrypt(salt)

  private def discardToken(path: Path): Path = {
    UriTokenRegex.findFirstMatchIn(path).map(m => m.group(1) + "/" + m.group(3)).getOrElse(path)
  }

  case class UsersBy(
    byId: Map[UserId, User],
    byIdPass: Map[(UserId, UserHashedPass), User]
  )

  object UsersBy {
    def apply(u: List[User]): UsersBy = {
      UsersBy(
        byId = u.groupBy(_.name).map { case (t, us) => (t, us.last) },
        byIdPass = u.groupBy(i => (i.name, i.hashedPass)).map { case (t, us) => (t, us.last) }
      )
    }
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
