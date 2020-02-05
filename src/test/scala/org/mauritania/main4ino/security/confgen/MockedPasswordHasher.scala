package tsec.passwordhashers.jca

import cats.effect.IO
import tsec.passwordhashers.{PasswordHash, PasswordHasher}

class MockedPasswordHasher extends PasswordHasher[IO, BCrypt] {
    def hashpw(p: Array[Char]): IO[PasswordHash[BCrypt]] = IO.pure(PasswordHash[BCrypt]("hashed-" + new String(p)))
    def hashpw(p: Array[Byte]): IO[PasswordHash[BCrypt]] = IO.pure(PasswordHash[BCrypt]("hashed-" + new String(p)))
    def checkpwBool(p: Array[Char], hash: PasswordHash[BCrypt]): IO[Boolean] = ???
    def checkpwBool(p: Array[Byte], hash: PasswordHash[BCrypt]): IO[Boolean] = ???
    private[tsec] def hashPassUnsafe(p: Array[Byte]): String = ???
    private[tsec] def checkPassUnsafe(p: Array[Byte], hash: PasswordHash[BCrypt]): Boolean = ???
}


