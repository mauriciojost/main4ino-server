val Http4sVersion = "0.18.14"
val Specs2Version = "4.2.0"
val LogbackVersion = "1.2.3"
val DoobieVersion = "0.5.2"
val H2Version = "1.4.192"
val FlywayVersion = "4.2.0"
val CirceVersion = "0.6.1"
val PureConfigVersion = "0.9.1"
lazy val ScalaTestVersion = "3.0.4"
lazy val ScalaMockVersion = "4.0.0"

lazy val root = (project in file("."))
  .settings(
    organization := "org.mauritania",
    name := "botinobe",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.6",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.specs2" %% "specs2-core" % Specs2Version % "test",
      "org.reactormonk" %% "cryptobits" % "1.2",
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion % "test",
      "org.tpolecat" %% "doobie-core" % DoobieVersion,
      "org.tpolecat" %% "doobie-h2" % DoobieVersion,
      "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
      "com.h2database" % "h2" % H2Version,
      "org.flywaydb" % "flyway-core" % FlywayVersion,
      "com.github.pureconfig" %% "pureconfig" % PureConfigVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
      "org.scalamock" %% "scalamock" % ScalaMockVersion % "test"
    ),
    parallelExecution in Test := false
  )



