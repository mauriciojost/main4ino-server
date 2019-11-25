import sbt._

object Dependencies {

    object Versions {
        val Http4sVersion = "0.21.0-RC4"
        val Specs2Version = "4.8.3"
        val DoobieVersion = "0.8.8"
        val H2Version = "1.4.200"
        val SqliteVersion = "3.30.1"
        val FlywayVersion = "6.2.2"
        val CirceVersion = "0.12.3"
        val PureConfigVersion = "0.12.2"
        val ScalaTestVersion = "3.1.0"
        val ScalaMockVersion = "4.4.0"
        val log4CatsSlf4jVersion = "1.0.1"
        val slf4jLog4j12Version = "1.7.30"
        val CryptobitsVersion = "1.3"
        val BcryptVersion = "4.1"
        val GfcSemverVersion = "0.0.5"
        val EnumeratumVersion = "1.5.15"
        val RefinedVersion = "0.9.12"
        val GfcSemver = "0.0.5"
    }

    val Dependencies = Seq(
          "org.http4s" %% "http4s-blaze-server" % Versions.Http4sVersion,
          "org.http4s" %% "http4s-circe" % Versions.Http4sVersion,
          "eu.timepit" %% "refined"                 % Versions.RefinedVersion,
          "eu.timepit" %% "refined-pureconfig"      % Versions.RefinedVersion,
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
          "org.tpolecat" %% "doobie-postgres" % Versions.DoobieVersion,
          "com.h2database" % "h2" % Versions.H2Version,
          "org.xerial" % "sqlite-jdbc" % Versions.SqliteVersion,
          "org.flywaydb" % "flyway-core" % Versions.FlywayVersion,
          "com.github.pureconfig" %% "pureconfig" % Versions.PureConfigVersion,
          "com.gilt" %% "gfc-semver" % Versions.GfcSemverVersion,
          "com.beachape" %% "enumeratum" % Versions.EnumeratumVersion,
          "com.beachape" %% "enumeratum-circe" % Versions.EnumeratumVersion,
          "org.scalatest" %% "scalatest" % Versions.ScalaTestVersion % "test",
          "org.scalamock" %% "scalamock" % Versions.ScalaMockVersion % "test"

  )
}
