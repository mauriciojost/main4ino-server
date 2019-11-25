val ProjectVersion = "0.1.0-SNAPSHOT"

val ScalaVersion = "2.12.10"

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

enablePlugins(JavaServerAppPackaging)

maintainer := "Mauricio Jost <mauriciojostx@gmail.com>"
packageSummary := "Properties management for main4ino-enabled embedded systems."
mainClass in Compile := Some("org.mauritania.main4ino.Server")

lazy val root = (project in file("."))
  .settings(
    organization := "org.mauritania",
    name := "main4ino-server",
    version := ProjectVersion,
    scalaVersion := ScalaVersion,
    scalacOptions ++= Seq(
      "-Ypartial-unification",
      "-language:higherKinds"
    ),
    libraryDependencies ++= Dependencies.Dependencies ++ tsec,
    parallelExecution in Test := false,

    coverageMinimum := 97,
    coverageFailOnMinimum := true

  )

val tsecV = "0.0.1-M11"
val tsec = Seq(
  "io.github.jmcardon" %% "tsec-common" % tsecV,
  "io.github.jmcardon" %% "tsec-password" % tsecV,
//  "io.github.jmcardon" %% "tsec-cipher-jca" % tsecV,
//  "io.github.jmcardon" %% "tsec-cipher-bouncy" % tsecV,
//  "io.github.jmcardon" %% "tsec-mac" % tsecV,
//  "io.github.jmcardon" %% "tsec-signatures" % tsecV,
//  "io.github.jmcardon" %% "tsec-hash-jca" % tsecV,
//  "io.github.jmcardon" %% "tsec-hash-bouncy" % tsecV,
//  "io.github.jmcardon" %% "tsec-libsodium" % tsecV,
  "io.github.jmcardon" %% "tsec-jwt-mac" % tsecV,
//  "io.github.jmcardon" %% "tsec-jwt-sig" % tsecV,
//  "io.github.jmcardon" %% "tsec-http4s" % tsecV
)
