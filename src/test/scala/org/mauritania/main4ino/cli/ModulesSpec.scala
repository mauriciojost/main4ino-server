package org.mauritania.main4ino.cli

import cats.Id
import org.mauritania.main4ino.SyncId
import org.mauritania.main4ino.cli.Data.AddRawUserParams
import org.mauritania.main4ino.cli.Modules.ConfigsAppErr
import org.scalatest.{Matchers, WordSpec}
import org.mauritania.main4ino.security.Fixtures._
import org.mauritania.main4ino.security.{Authentication, User}

class ModulesSpec extends WordSpec with Matchers with SyncId {

  "A config handler" should {

    "add a user correctly" in {
      val c = new ConfigsAppErr[Id]()
      val baseConfig = DefaultSecurityConfig
      val newUser = AddRawUserParams("pepe", "toto", "pepe@zzz.com", List("/"))
      val newConf = c.addUser(baseConfig, newUser)

      val ExpectedNewUserEntry = User(
        name = "pepe",
        hashedpass = Authentication.hashPassword("toto", Salt),
        email = "pepe@zzz.com",
        granted = List("/")
      )

      newConf shouldBe baseConfig.copy(users = ExpectedNewUserEntry :: baseConfig.users)

    }

  }
}
