package org.mauritania.main4ino.security.confgen

import java.nio.file.{Files, Paths}

import com.typesafe.config.ConfigFactory
import enumeratum.Circe
import io.circe.generic.auto._
import io.circe.jawn.decode
import org.mauritania.main4ino.{DecodersIO, TmpDirCtx}
import org.mauritania.main4ino.security.MethodRight.RW
import org.mauritania.main4ino.security.{Config, MethodRight}

import scala.io.Source
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pureconfig.ConfigSource

class ClientSpec extends AnyWordSpec with Matchers with DecodersIO with TmpDirCtx {
  val User1Cmd = "newuser newpass newuser@zzz.com /api/v1/time /"
  val User2Cmd = "newuser2 newpass2 newuser2@zzz.com /api"

  import org.mauritania.main4ino.helpers.ConfigLoader.CirceImplicits._
  import org.mauritania.main4ino.helpers.ConfigLoader.PureConfigImplicits._

  "The client" should {
    "add correctly a user and then another one" in {
      withTmpDir { tmp =>

        val input1 = Paths.get("src", "main", "resources", "defaultconfig", "security.conf")
        val modif1 = Paths.get("src", "test", "resources", "configs", "4", "adduser1.conf")
        val output1 = Files.createTempFile(tmp, "output-", ".conf")

        System.setProperty("input", input1.toFile.getAbsolutePath)
        System.setProperty("modif", modif1.toFile.getAbsolutePath)
        System.setProperty("output", output1.toFile.getAbsolutePath)
        ConfigFactory.invalidateCaches() // force reload of java properties
        Client.main(Array())

        val fileContent1 = Source.fromFile(output1.toFile).mkString
        val config1 = decode[Config](fileContent1).toTry.get
        config1.users.map(_.name).toSet shouldBe Set(/* just added */ "newuser", "admin")
        config1.users.map(_.email).toSet shouldBe Set(/* just added */ "newuser@zzz.com", "admin@zzz.com")
        config1.users.map(_.granted.toSet).toSet shouldBe Set(
          /* just added */ Set("/api/v1/time" -> RW, "/" -> RW),
          Set("/" -> RW)
        )

        val input2 = output1
        val modif2 = Paths.get("src", "test", "resources", "configs", "4", "adduser2.conf")
        val output2 = output1

        System.setProperty("input", input2.toFile.getAbsolutePath)
        System.setProperty("modif", modif2.toFile.getAbsolutePath)
        System.setProperty("output", output2.toFile.getAbsolutePath)
        ConfigFactory.invalidateCaches() // force reload of java properties
        Client.main(Array())

        val fileContent2 = Source.fromFile(output2.toFile).mkString
        val config2 = decode[Config](fileContent2).toOption.get
        config2.users.map(_.name).toSet shouldBe Set("newuser2", "newuser", "admin")
        config2.users.map(_.email).toSet shouldBe Set("newuser2@zzz.com", "newuser@zzz.com", "admin@zzz.com")
        config2.users.map(_.granted.toSet).toSet shouldBe Set(
          Set("/api" -> RW),
          Set("/api/v1/time" -> RW, "/" -> RW),
          Set("/" -> RW)
        )
      }
    }
  }
}
