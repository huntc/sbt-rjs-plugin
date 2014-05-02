lazy val root = (project in file(".")).enablePlugins(SbtWeb)

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

libraryDependencies ++= Seq(
  "org.webjars" % "requirejs" % "2.1.11-1",
  "org.webjars" % "underscorejs" % "1.6.0-1"
)

