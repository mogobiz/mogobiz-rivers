organization := "com.mogobiz.rivers"

name := "cfp"

version := "0.1-SNAPSHOT"

logLevel in Global := Level.Info

val scalaV = "2.11.2"

crossScalaVersions := Seq(scalaV)

scalaVersion := scalaV

val akkaV = "2.3.9"

val sprayV = "1.3.2"

val specs2V = "2.4"

resolvers += Resolver.sonatypeRepo("releases")

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies ++= Seq(
  "org.scala-lang"           % "scala-library"  % scalaV,
  "org.json4s"              %% "json4s-native"  % "3.2.9",
  "org.json4s"              %% "json4s-jackson" % "3.2.9",
  "com.typesafe.akka"       %% "akka-actor"     % akkaV,
  "com.typesafe.akka"       %% "akka-testkit"   % akkaV,
  "com.typesafe.akka"       %% "akka-stream-experimental" % "1.0-M3",
  "io.spray"                %% "spray-client"   % sprayV,
  "org.specs2"              %% "specs2"         % specs2V % "test",
  "ch.qos.logback"          % "logback-classic" % "1.1.2" % "provided",
  "net.databinder.dispatch" %% "dispatch-core"  % "0.11.2"
)


// unmanagedBase := baseDirectory.value / "lib"
