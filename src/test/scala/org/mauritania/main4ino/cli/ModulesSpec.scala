package org.mauritania.main4ino.cli

import java.nio.file.{Files, Paths}

import cats.Id
import org.mauritania.main4ino.SyncId
import org.mauritania.main4ino.cli.Actions.AddRawUser
import org.mauritania.main4ino.cli.Modules.{ConfigsAppErr, FilesystemSync}
import org.scalatest.{Matchers, WordSpec}
import org.mauritania.main4ino.security.Fixtures._
import org.mauritania.main4ino.security.{Authentication, User}

class ModulesSpec extends WordSpec with Matchers with SyncId {

  "A config handler" should {

    "add a user" in {
      val c = new ConfigsAppErr[Id]()
      val baseConfig = DefaultSecurityConfig
      val newUser = AddRawUser("pepe", "toto", "pepe@zzz.com", List("/"))
      val newConf = c.performAction(baseConfig, newUser)

      val ExpectedNewUserEntry = User(
        name = "pepe",
        hashedpass = Authentication.hashPassword("toto", Salt),
        email = "pepe@zzz.com",
        granted = List("/")
      )

      newConf shouldBe baseConfig.copy(users = ExpectedNewUserEntry :: baseConfig.users)

    }

  }

  "A filesystem handler" should {

    "read a file" in {
      val fs = new FilesystemSync[Id]()
      val p = Paths.get("src", "test", "resources", "atextfile.txt")
      val content = fs.readFile(p)
      content shouldBe "file content"
    }

    "write a file" in {
      val fs = new FilesystemSync[Id]()
      val output = Files.createTempFile("write", "tmp")
      val content = "0123"
      fs.writeFile(output, content)
      output.toFile.length() shouldBe content.length
      output.toFile.deleteOnExit()
    }

  }
}
