package org.mauritania.main4ino.security.confgen

import java.nio.file.{Files, Paths}

import cats.effect.{IO, Sync}
import org.mauritania.main4ino.DecodersIO
import org.mauritania.main4ino.security.confgen.Actions.{AddRawUser, AddRawUsers, Identity}
import org.mauritania.main4ino.security.confgen.Modules.{ConfigsMonad, FilesystemSync}
import org.mauritania.main4ino.security.Fixtures._
import org.mauritania.main4ino.security.MethodRight.RW
import org.mauritania.main4ino.security.{MethodRight, User}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import tsec.passwordhashers.jca.{BCrypt, MockedPasswordHasher}
import tsec.passwordhashers.PasswordHash

class ModulesSpec extends AnyWordSpec with Matchers with DecodersIO {

  "A config handler" should {

    "apply identity if no operation" in {
      implicit val ph = new MockedPasswordHasher()
      val c = new ConfigsMonad[IO]()
      val baseConfig = DefaultSecurityConfig
      val newConf = c.performActions(baseConfig, List(Identity)).unsafeRunSync()

      newConf shouldBe baseConfig
    }

    "add a user" in {
      implicit val ph = new MockedPasswordHasher()
      val c = new ConfigsMonad[IO]()
      val baseConfig = DefaultSecurityConfig
      val addNewUser = AddRawUsers(List(AddRawUser("pepe", "toto", "pepe@zzz.com", Map[String, MethodRight]("/" -> RW))))
      val newConf = c.performActions(baseConfig, List(addNewUser)).unsafeRunSync()

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
