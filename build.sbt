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
    libraryDependencies ++= Dependencies.Dependencies,
    parallelExecution in Test := false,

    coverageMinimum := 97,
    coverageFailOnMinimum := true

  )



