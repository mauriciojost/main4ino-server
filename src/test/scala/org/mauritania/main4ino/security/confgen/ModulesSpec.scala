package org.mauritania.main4ino.security.confgen

import java.nio.file.{Files, Paths}

import cats.effect.{IO, Sync}
import org.mauritania.main4ino.DecodersIO
import org.mauritania.main4ino.security.Auther.UserHashedPass
import org.mauritania.main4ino.security.confgen.Actions.{AddRawUser, AddRawUsers}
import org.mauritania.main4ino.security.confgen.Modules.{ConfigsMonad, FilesystemSync}
import org.mauritania.main4ino.security.Fixtures._
import org.mauritania.main4ino.security.MethodRight.RW
import org.mauritania.main4ino.security.{Auther, MethodRight, User}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import tsec.passwordhashers.jca.{BCrypt, MockedPasswordHasher}
import tsec.passwordhashers.{PasswordHash, PasswordHasher}

class ModulesSpec extends AnyWordSpec with Matchers with DecodersIO {

  "A config handler" should {

    // Replace all asInstanceOf with PasswordHash[BCrypt](...)
    "add a user" in {
      implicit val ph = new MockedPasswordHasher()
      val c = new ConfigsMonad[IO]()
      val baseConfig = DefaultSecurityConfig
      val newUser = AddRawUsers(List(AddRawUser("pepe", "toto", "pepe@zzz.com", Map[String, MethodRight]("/" -> RW))))
      val newConf = c.performAction(baseConfig, newUser).unsafeRunSync()

      val ExpectedNewUserEntry = User(
        name = "pepe",
        hashedpass = PasswordHash[BCrypt]("hashed-toto"),
        email = "pepe@zzz.com",
        granted = Map[String, MethodRight]("/" -> RW)
      )

      newConf shouldBe baseConfig.copy(users = ExpectedNewUserEntry :: baseConfig.users)

    }

  }

  "A filesystem handler" should {

    "read a file" in {
      val fs = new FilesystemSync[IO]()
      val p = Paths.get("src", "test", "resources", "misc", "atextfile.txt")
      val content = fs.readFile(p)
      content.unsafeRunSync() shouldBe "file content"
    }

    "write a file" in {
      val fs = new FilesystemSync[IO]()
      val output = Files.createTempFile("write", "tmp")
      val content = "0123"
      fs.writeFile(output, content)
      output.toFile.length() shouldBe content.length
      output.toFile.deleteOnExit()
    }

  }
}
