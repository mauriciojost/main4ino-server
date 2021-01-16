val ScalaVersion = "2.12.10"

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full)

enablePlugins(JavaServerAppPackaging, UniversalDeployPlugin)

maintainer := "Mauricio Jost <mauriciojostx@gmail.com>"
packageSummary := "Properties management for main4ino-enabled embedded systems."
mainClass in Compile := Some("org.mauritania.main4ino.Server")

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoOptions += BuildInfoOption.ToJson,
    buildInfoPackage := "org.mauritania.main4ino"
  )
  .settings(
    organization := "org.mauritania",
    name := "main4ino-server",
    scalaVersion := ScalaVersion,
    scalacOptions ++= Seq(
      "-Ypartial-unification",
      "-language:higherKinds"
    ),
    libraryDependencies ++= Dependencies.Dependencies,
    parallelExecution in Test := true,

    coverageMinimum := 97,
    coverageFailOnMinimum := true

  )

