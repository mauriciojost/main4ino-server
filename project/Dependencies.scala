import sbt._

object Dependencies {

    object Versions {
        val Http4sVersion = "0.21.13"
        val Specs2Version = "4.10.5"
        val DoobieVersion = "0.9.4"
        val H2Version = "1.4.200"
        val SqliteVersion = "3.32.3.1"
        val FlywayVersion = "7.2.1"
        val CirceVersion = "0.13.0"
        val PureConfigVersion = "0.14.0"
        val ScalaTestVersion = "3.2.3"
        val ScalaMockVersion = "5.1.0"
        val FreePortFinder = "1.1.1"
        val log4CatsSlf4jVersion = "1.1.1"
        val slf4jLog4j12Version = "1.7.30"
        val CryptobitsVersion = "1.3"
        val GfcSemverVersion = "0.0.5"
        val EnumeratumVersion = "1.6.1"
        val RefinedVersion = "0.9.18"
        val GfcSemver = "0.0.5"
        val TsecVersion = "0.2.1"
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
          "org.scalamock" %% "scalamock" % Versions.ScalaMockVersion % "test",
          "me.alexpanov" % "free-port-finder" % Versions.FreePortFinder % "test",
          "io.github.jmcardon" %% "tsec-common" % Versions.TsecVersion,
          "io.github.jmcardon" %% "tsec-password" % Versions.TsecVersion,
          "io.github.jmcardon" %% "tsec-jwt-mac" % Versions.TsecVersion
  )
}
