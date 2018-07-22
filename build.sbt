val Http4sVersion = "0.18.14"
val Specs2Version = "4.2.0"
val LogbackVersion = "1.2.3"

lazy val root = (project in file("."))
  .settings(
    organization := "org.mauritania",
    name := "botinobe",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.6",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server"                % Http4sVersion,
      "org.http4s"      %% "http4s-circe"                       % Http4sVersion,
      "io.circe"        %% "circe-generic"                      % "0.6.1", // Optional for auto-derivation of JSON codecs
      "io.circe"        %% "circe-literal"                      % "0.6.1", // Optional for string interpolation to JSON model

      "org.http4s"      %% "http4s-json4s-native"               % Http4sVersion,
      "org.http4s"      %% "http4s-json4s-jackson"              % Http4sVersion,
      "org.http4s"      %% "http4s-argonaut"                    % Http4sVersion,
      "com.github.alexarchambault" %% "argonaut-shapeless_6.2"  % "1.2.0-M6",

      "org.http4s"      %% "http4s-dsl"                         % Http4sVersion,
      "org.specs2"      %% "specs2-core"          % Specs2Version % "test",
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion


      )
  )



