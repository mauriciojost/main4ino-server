package org.mauritania.main4ino.security

import java.time.Clock

import cats.effect.Sync
import org.http4s.{AuthedRequest, BasicCredentials, Credentials, Headers, Method, Request, Uri}
import org.http4s.Uri.Path
import org.http4s.util.CaseInsensitiveString
import org.mauritania.main4ino.security.Auther.{AccessAttempt, ErrorMsg, UserSession}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.headers.Authorization
import org.mauritania.main4ino.security.Config.UsersBy
import cats._
import cats.implicits._
import cats._
import cats.syntax._
import cats.instances._
import cats.implicits._
import cats.effect.{IO, Sync}
import org.http4s.{BasicCredentials, Headers, Request, Uri}
import org.http4s.Uri.Path
import org.http4s.util.CaseInsensitiveString
import org.mauritania.main4ino.security.Auther.{AccessAttempt, UserSession}
import org.http4s.headers.Authorization
import org.mauritania.main4ino.security.Config.UsersBy
import tsec.jws.mac.JWTMac
import tsec.jwt.JWTClaims
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers._
import tsec.passwordhashers.jca._

import scala.concurrent.duration._
import scala.util.Try
import scala.util.matching.Regex


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
  def authenticateAndCheckAccess(request: Request[F]): F[Either[ErrorMsg, AuthedRequest[F, User]]] = {
    for {
      logger <- Slf4jLogger.fromClass[F](getClass)
      attempt <- Auther.authenticateAndCheckAccess(config.usersBy, config.encryptionConfig, request.headers, request.method, request.uri)
      _ <- logger.debug(s">>> Authentication: ${attempt.map(_.id)}")
      authedRequest = attempt.map { u =>
        AuthedRequest(
          context = u,
          req = request
            .withPathInfo(Auther.dropTokenAndSessionFromPath(request.pathInfo))
        )
      }
    } yield authedRequest
  }

  /**
    * Provide a session given a user
    */
  def generateSession(user: User): F[UserSession] =
    Auther.sessionFromUser[F](user, config.privateKeyBits, config.nonceStartupTime)

}

object Auther {

  type UserId = String // username
  type UserHashedPass = PasswordHash[BCrypt]
//  type UserHashedPass = String
  type UserSession = String // generated after login

  type ErrorMsg = String
  type AccessAttempt = Either[ErrorMsg, User]
  type AuthenticationAttempt = Either[ErrorMsg, User]

  private final val HeaderSession = CaseInsensitiveString("session")
  private final val UriTokenRegex = ("^(.*?)/token/(.*?)/(.*)$").r
  private final val UriSessionRegex = ("^(.*?)/session/(.*?)/(.*)$").r
  private final val GroupPre = 1
  private final val GroupThe = 2
  private final val GroupPos = 3

  def authenticateAndCheckAccess[F[_]: Sync : Monad](usersBy: UsersBy, encry: EncryptionConfig, headers: Headers, method: Method, uri: Uri)(implicit H: PasswordHasher[F, BCrypt]): F[AccessAttempt] = {
    val resource = uri.path
    val credentials = userCredentialsFromRequest(encry, headers, uri)
    val c = credentials.map(c => c._2.map(p => (c._1, p))).sequence
    val session = sessionFromRequest(headers, uri)
    for {
      cc <- c
      user <- authenticatedUserFromSessionOrCredentials(encry, usersBy, session, cc)
    } yield user.flatMap(u => checkAccess(u, method, resource))
  }

  /**
    * Create a session for a given user given a private key and a timestamp
    *
    * @param user user for whom we want to create a session
    * @param privateKey private key used for encryption of the session id to be generated
    * @param time time used to generate the session id
    * @return a user session id
    */
  def sessionFromUser[F[_]: Sync : Monad](user: User, privateKey: CryptoBits, time: Clock): F[UserSession] = {
    val claims = JWTClaims(subject = Some(user.id))
    for {
      key             <- HMACSHA256.buildKey[F](privateKey)
      stringjwt       <- JWTMac.buildToString[F, HMACSHA256](claims, key)
    } yield stringjwt
  }

  def userIdFromSession[F[_] : Sync : Monad](session: UserSession, privateKey: CryptoBits): F[Option[String]] = {
    for {
      key             <- HMACSHA256.buildKey[F](privateKey)
      parsed          <- JWTMac.verifyAndParse[F, HMACSHA256](session, key)
    } yield parsed.body.subject
  }

  def userFromSession[F[_]: Sync](session: UserSession, privateKey: CryptoBits, usersBy: UsersBy): F[Option[User]] = {
    for {
      id <- userIdFromSession(session, privateKey)
    } yield id.flatMap(usersBy.byId.get)

  }

  def authenticatedUserFromSessionOrCredentials[F[_]: Sync](encry: EncryptionConfig, usersBy: UsersBy,
                                                session: Option[UserSession], creds: Option[(UserId, UserHashedPass)]): F[AuthenticationAttempt] = {
    val a = creds.flatMap(usersBy.byIdPass.get)
    for {
      b <- session.map(s => userFromSession(s, encry.pkey, usersBy)).sequence.map(_.flatten)
    } yield a.orElse(b).toRight(s"Could not find related user (user:${creds.map(_._1)} / session:$session)")
  }

  /**
    * Check if a user can access a given resource
    */
  def checkAccess(user: User, method: Method, resourceUriPath: Path): AccessAttempt = {
    user.authorized(method, dropTokenAndSessionFromPath(resourceUriPath))
      .toRight(s"User '${user.name}' is not authorized to ${method} '${resourceUriPath}'")
  }

  def userCredentialsFromRequest[F[_]](encry: EncryptionConfig, headers: Headers, uri: Uri)(implicit H: PasswordHasher[F, BCrypt]): Option[(UserId, F[UserHashedPass])] = {
    // Basic auth
    val credsFromHeader = headers.get(Authorization).collect {
      case Authorization(BasicCredentials(username, password)) => (username, hashPassword(password))
    }
    // URI auth: .../token/<token>/... authentication (some services
    // like IFTTT or devices ESP8266 HTTP UPDATE do not support headers, but only URI credentials...)
    val tokenFromUri = UriTokenRegex.findFirstMatchIn(uri.path).flatMap(a => Try(a.group(GroupThe)).toOption)
    val validCredsFromUri = tokenFromUri
      .map(t => BasicCredentials(t))
      .map(c => (c.username, hashPassword(c.password)))

    credsFromHeader
      .orElse(validCredsFromUri)
  }

  private[security] def sessionFromRequest(headers: Headers, uri: Uri): Option[UserSession] = {

    // URI auth: .../session/<session>/... authentication (some services
    // like IFTTT or devices ESP8266 HTTP UPDATE do not support headers, but only URI credentials...)
    val uriSession = UriSessionRegex.findFirstMatchIn(uri.path).flatMap(a => Try(a.group(GroupThe)).toOption)
    val headerSession = headers.get(HeaderSession).map(_.value)
    headerSession.orElse(uriSession)
  }

  def hashPassword[F[_]](password: String)(implicit P: PasswordHasher[F, BCrypt]): F[UserHashedPass] = BCrypt.hashpw[F](password)

  private[security] def dropTokenAndSessionFromPath(path: Path): Path = {
    def drop(r: Regex, p: Path) = r.findFirstMatchIn(p).map(m => m.group(GroupPre) + "/" + m.group(GroupPos)).getOrElse(p)
    val res = drop(UriSessionRegex, drop(UriTokenRegex, path))
    res
  }

  /**
    * Encryption configuration
    *
    * @param pkey private key used to generate sessions
    * @param salt salt used to hash passwords
    */
  case class EncryptionConfig(
    pkey: CryptoBits
  )

  type CryptoBits = Array[Byte]
}
