import sbt._

object Dependencies {

    object Versions {
        val Http4sVersion = "0.18.14"
        val Specs2Version = "4.2.0"
        val LogbackVersion = "1.2.3"
        val DoobieVersion = "0.5.2"
        val H2Version = "1.4.192"
        val FlywayVersion = "4.2.0"
        val CirceVersion = "0.9.3"
        val PureConfigVersion = "0.10.2"
        val ScalaTestVersion = "3.0.4"
        val ScalaMockVersion = "4.0.0"
        val log4CatsSlf4jVersion = "0.1.0"
        val slf4jLog4j12Version = "1.8.0-beta2"
        val CryptobitsVersion = "1.2"
        val BcryptVersion = "3.1"
        val GfcSemver = "0.0.5"
    }

    val Dependencies = Seq(
          "org.reactormonk" %% "cryptobits" % Versions.CryptobitsVersion,
          "com.github.t3hnar" %% "scala-bcrypt" % Versions.BcryptVersion,
          "org.http4s" %% "http4s-blaze-server" % Versions.Http4sVersion,
          "org.http4s" %% "http4s-circe" % Versions.Http4sVersion,
          "io.circe" %% "circe-core" % Versions.CirceVersion,
          "io.circe" %% "circe-generic" % Versions.CirceVersion,
          "io.circe" %% "circe-parser" % Versions.CirceVersion,
          "io.chrisdavenport" %% "log4cats-slf4j" % Versions.log4CatsSlf4jVersion,
          "org.slf4j" % "slf4j-log4j12" % Versions.slf4jLog4j12Version,
          "org.http4s" %% "http4s-dsl" % Versions.Http4sVersion,
          "org.specs2" %% "specs2-core" % Versions.Specs2Version % "test",
          "org.http4s" %% "http4s-blaze-client" % Versions.Http4sVersion % "test",
          "org.tpolecat" %% "doobie-core" % Versions.DoobieVersion,
          "org.tpolecat" %% "doobie-h2" % Versions.DoobieVersion,
          "org.tpolecat" %% "doobie-hikari" % Versions.DoobieVersion,
          "com.h2database" % "h2" % Versions.H2Version,
          "org.flywaydb" % "flyway-core" % Versions.FlywayVersion,
          "com.github.pureconfig" %% "pureconfig" % Versions.PureConfigVersion,
          "com.gilt" %% "gfc-semver" % Versions.GfcSemver,
          "org.scalatest" %% "scalatest" % Versions.ScalaTestVersion % "test",
          "org.scalamock" %% "scalamock" % Versions.ScalaMockVersion % "test"
        )
}