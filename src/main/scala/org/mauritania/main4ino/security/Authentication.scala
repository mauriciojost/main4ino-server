package org.mauritania.main4ino.security

import java.time.Clock

import cats.effect.IO
import org.http4s.{Headers, Request, Uri}
import org.http4s.Uri.Path
import org.http4s.util.CaseInsensitiveString
import org.mauritania.main4ino.security.Authentication.{AccessAttempt, UserSession}
import org.reactormonk.CryptoBits
import com.github.t3hnar.bcrypt._

import scala.util.Try

trait Authentication[F[_]] {

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
  type UserPass = String // password
  type UserHashedPass = String // hashed password
  type UserSession = String // generated after login

  type AccessErrorMsg = String
  type AccessAttempt = Either[AccessErrorMsg, User]

  final val UserIdExpr = "[a-zA-Z0-9]{4,30}"
  final val UserPassExpr = "[^:/]{8,30}"
  final val HeaderUserIdPass = CaseInsensitiveString("Credentials")
  final val HeaderSession = CaseInsensitiveString("Session")
  final val HeaderUserIdPassSeparator = ":"
  private final val HeaderUserIdPassRegex = {"^(" + UserIdExpr + ")" + HeaderUserIdPassSeparator + "(" + UserPassExpr + ")$"}.r

  // TODO authenticated != authorized, review whole file to ensure naming is correct
  def authenticateUser(usersBy: UsersBy, encry: EncryptionConfig, headers: Headers, uri: Uri, resource: Path): AccessAttempt = {
    val userCredentials = userCredentialsFromCredentialsRequest(encry, headers, uri)
    val userIdSession = userIdFromValidSessionRequest(headers)
    val user = authenticatedUserFromSessionOrCredentials(encry, usersBy, userIdSession, userCredentials)
    val authorized = authorizedUserFromToken(user, resource)
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

  def authorizedUserFromToken(user: AccessAttempt, uriPath: Path): AccessAttempt = {
    for {
      u <- user
      ua <- u.authorized(uriPath).toRight(s"User ${u.name} is not authorized to access ${uriPath}")
    } yield (ua)
  }

  def userCredentialsFromCredentialsRequest(encry: EncryptionConfig, headers: Headers, uri: Uri): Option[(UserId, UserHashedPass)] = {
    val rawCredsFromHeader = headers.get(HeaderUserIdPass)
    val parsedCredsFromHeader = rawCredsFromHeader.flatMap(v => HeaderUserIdPassRegex.findFirstMatchIn(v.value))
    val validCredsFromHeader = parsedCredsFromHeader.flatMap(a => Try((a.group(1), passHash(a.group(2), encry.salt))).toOption)
    val rawCredsFromUri = uri.authority.flatMap(_.userInfo)
    val parsedCredsFromUri = rawCredsFromUri.flatMap(u => HeaderUserIdPassRegex.findFirstMatchIn(u))
    val validCredsFromUri = parsedCredsFromUri.flatMap(a => Try((a.group(1), passHash(a.group(2), encry.salt))).toOption)
    validCredsFromHeader.orElse(validCredsFromUri)
  }

  def userIdFromValidSessionRequest(headers: Headers): Option[UserSession] = headers.get(HeaderSession).map(_.value)

  def headerUserId(id: UserId, pass: UserPass): String = id + HeaderUserIdPassSeparator + pass

  def passHash(clearPass: String, salt: String): String = clearPass.bcrypt(salt)

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
    * @param pkey private key used to generate sessions
    * @param salt salt used to hash passwords
    */
  case class EncryptionConfig(
    pkey: CryptoBits,
    salt: String
  )
}
