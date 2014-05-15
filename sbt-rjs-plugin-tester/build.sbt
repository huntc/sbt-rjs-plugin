import RjsKeys._

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

libraryDependencies ++= Seq(
  "org.webjars" % "requirejs" % "2.1.11-1",
  "org.webjars" % "underscorejs" % "1.6.0-1"
)

pipelineStages := Seq(rjs)

appBuildProfile := s"""|({
                       |  appDir: "${appDir.value}",
                       |  baseUrl: "js",
                       |  dir: "${dir.value}",
                       |  generateSourceMaps: true,
                       |  mainConfigFile: "${appDir.value / "js" / "main.js"}",
                       |  modules: [{
                       |    name: "main"
                       |  }],
                       |  onBuildWrite: ${buildWriter.value},
                       |  optimize: "uglify2",
                       |  paths: ${RjsJson.toJsonObj(webJarModuleIds.value.map(m => m -> "empty:"))},
                       |  preserveLicenseComments: false
                       |})""".stripMargin