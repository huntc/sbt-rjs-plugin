import com.typesafe.sbt.jse.SbtJsEnginePlugin._
import com.typesafe.sbt.web.SbtWebPlugin

lazy val root = project.in(file(".")).addPlugins(SbtWebPlugin)

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

libraryDependencies ++= Seq(
  "org.webjars" % "requirejs" % "2.1.11-1",
  "org.webjars" % "underscorejs" % "1.6.0-1"
)

