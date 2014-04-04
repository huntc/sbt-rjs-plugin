sbtPlugin := true

organization := "com.typesafe.sbt"

name := "sbt-rjs"

version := "1.0.0-M2"

scalaVersion := "2.10.3"

scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "org.webjars" % "rjs" % "2.1.11-1-trireme"
)

resolvers ++= Seq(
  "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.url("sbt snapshot plugins", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
  Resolver.sonatypeRepo("snapshots"),
  "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/",
  Resolver.mavenLocal
)

addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.0.0-M2")

scriptedSettings

scriptedLaunchOpts <+= version apply { v => s"-Dproject.version=$v" }

// FIXME: Working around https://github.com/sbt/sbt/issues/1156#issuecomment-39317363
isSnapshot := true

publishMavenStyle := false

publishTo := {
  val isSnapshot = version.value.contains("-SNAPSHOT")
  val scalasbt = "http://repo.scala-sbt.org/scalasbt/"
  val (name, url) = if (isSnapshot)
    ("sbt-plugin-snapshots", scalasbt + "sbt-plugin-snapshots")
  else
    ("sbt-plugin-releases", scalasbt + "sbt-plugin-releases")
  Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}