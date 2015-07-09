name := "play-basic-authentication-filter"

organization := "net.kaliber"

scalaVersion := "2.11.6"

releaseCrossBuild := true

crossScalaVersions := Seq("2.10.4", scalaVersion.value)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.4.0" % "provided",
  "com.typesafe.play" %% "play-test" % "2.4.0" % "test",
  "com.typesafe.play" %% "play-specs2" % "2.4.0" % "test",
  "org.specs2" %% "specs2-core" % "3.6.2" % "test"
)

resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases" /* specs2-core depends on scalaz-stream */
)

publishTo := {
  val repo = if (version.value endsWith "SNAPSHOT") "snapshot" else "release"
  Some("Kaliber Internal " + repo.capitalize + " Repository" at "https://jars.kaliber.io/artifactory/libs-" + repo + "-local")
}
