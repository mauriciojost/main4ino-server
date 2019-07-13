package org.mauritania.main4ino.cli

import java.nio.file.{Files, Paths}

import io.circe.generic.auto._
import io.circe.jawn.decode
import org.mauritania.main4ino.DecodersIO
import org.mauritania.main4ino.security.Config
import org.scalatest.{Matchers, WordSpec}

import scala.io.Source

class ClientSpec extends WordSpec with Matchers with DecodersIO {
  val User1Cmd = "newuser newpass newuser@zzz.com /api/v1/time /"
  val User2Cmd = "newuser2 newpass2 newuser2@zzz.com /api"

  "The client" should {
    "add correctly a user and then another one" in {

      val input1 = Paths.get("src", "main", "resources", "security.conf")
      val modif1 = Paths.get("src", "test", "resources", "adduser1.conf")
      val output1 = Files.createTempFile("security", "conf")
      output1.toFile.deleteOnExit()
      val args1 = Array(input1.toFile.getAbsolutePath, modif1.toFile.getAbsolutePath, output1.toFile.getAbsolutePath)

      Client.main(args1)

      val fileContent1 = Source.fromFile(output1.toFile).mkString
      val config1 = decode[Config](fileContent1).toOption.get
      config1.users.map(_.name).toSet shouldBe Set(/* just added */ "newuser", "admin")
      config1.users.map(_.email).toSet shouldBe Set(/* just added */ "newuser@zzz.com", "admin@zzz.com")
      config1.users.map(_.granted.toSet).toSet shouldBe Set(
        /* just added */ Set("/api/v1/time", "/"),
        Set("/")
      )

      val input2 = output1
      val modif2 = Paths.get("src", "test", "resources", "adduser2.conf")
      val output2 = output1
      val args2 = Array(input2.toFile.getAbsolutePath, modif2.toFile.getAbsolutePath, output2.toFile.getAbsolutePath)

      Client.main(args2)

      val fileContent2 = Source.fromFile(output2.toFile).mkString
      val config2 = decode[Config](fileContent2).toOption.get
      config2.users.map(_.name).toSet shouldBe Set("newuser2", "newuser", "admin")
      config2.users.map(_.email).toSet shouldBe Set("newuser2@zzz.com", "newuser@zzz.com", "admin@zzz.com")
      config2.users.map(_.granted.toSet).toSet shouldBe Set(
        Set("/api"),
        Set("/api/v1/time", "/"),
        Set("/")
      )
    }
  }
}
