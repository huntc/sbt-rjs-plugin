package com.typesafe.sbt.rjs

import sbt._
import sbt.Keys._
import com.typesafe.sbt.web.SbtWeb
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.jse.{SbtJsEngine, SbtJsTask}
import java.nio.charset.Charset
import java.io.{BufferedReader, InputStreamReader}
import org.webjars.WebJarAssetLocator
import java.util.regex.Pattern

object Import {

  val rjs = TaskKey[Pipeline.Stage]("rjs", "Perform RequireJs optimization on the asset pipeline.")

  object RjsKeys {
    val appBuildProfile = TaskKey[String]("rjs-app-build-profile", "The project build profile contents.")
    val appDir = SettingKey[File]("rjs-app-dir", "The top level directory that contains your app js files.")
    val buildWriter = TaskKey[String]("rjs-build-writer", "The project build writer JS that is responsible for writing out source files in rjs.")
    val dir = SettingKey[File]("rjs-dir", "By default, all modules are located relative to this path.")
    val paths = TaskKey[Set[(String, String)]]("rjs-paths", "A set of RequireJS path mappings. By default all WebJar libraries are made available from a CDN and their mappings can be found here (unless the cdn is set to None).")
    val webjarCdn = SettingKey[Option[String]]("rjs-webjar-cdn", "A CDN to be used for locating WebJars. By default jsdelivr is used.")
    val webJarModuleIds = TaskKey[Set[String]]("rjs-webjar-module-ids", "A sequence of webjar module ids to be used.")
  }

  object RjsJson {
    def toJsonObj(entries: Set[(String, String)]): String = entries.map {
      case (key, value) => s""""$key":"$value" """
    }.mkString("{", ",", "}")
  }

}

object SbtRjs extends AutoPlugin {

  override def requires = SbtJsTask

  override def trigger = AllRequirements

  val autoImport = Import

  import SbtWeb.autoImport._
  import WebKeys._
  import SbtJsEngine.autoImport.JsEngineKeys._
  import SbtJsTask.autoImport.JsTaskKeys._
  import autoImport._
  import RjsKeys._

  override def projectSettings = Seq(
    appBuildProfile := getAppBuildProfile.value,
    appDir := (resourceManaged in rjs).value / "appdir",
    buildWriter := getBuildWriter.value,
    dir := appDir.value / "build",
    excludeFilter in rjs := HiddenFileFilter,
    includeFilter in rjs := GlobFilter("*.js") | GlobFilter("*.css") | GlobFilter("*.map"),
    paths := getWebJarPaths.value,
    resourceManaged in rjs := webTarget.value / rjs.key.label,
    rjs := runOptimizer.dependsOn(webJarsNodeModules in Plugin).value,
    webjarCdn := Some("http://cdn.jsdelivr.net/webjars"),
    webJarModuleIds := getWebJarModuleIds.value
  )


  val Utf8 = Charset.forName("UTF-8")

  private def getAppBuildProfile: Def.Initialize[Task[String]] = Def.task {
    s"""|({
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
  }

  private def getBuildWriter: Def.Initialize[Task[String]] = Def.task {
    val source = getResourceAsList("buildWriter.js")
      .to[Vector]
      .dropRight(1) :+ s"""})(
          "${webModulesLib.value}/",
          ${RjsJson.toJsonObj(paths.value)},
          ${RjsJson.toJsonObj(webJarModuleIds.value.map(m => m -> m))}
          )"""
    source.mkString("\n")
  }

  private def getResourceAsList(name: String): List[String] = {
    val in = SbtRjs.getClass.getClassLoader.getResourceAsStream(name)
    val reader = new BufferedReader(new InputStreamReader(in, Utf8))
    try {
      IO.readLines(reader)
    } finally {
      reader.close()
    }
  }

  private def getWebJarModuleIds: Def.Initialize[Task[Set[String]]] = Def.task {
    val DotJS = ".js"
    (webJars in Assets).value.collect {
      case f if f.name.endsWith(DotJS) => f.name.dropRight(DotJS.length)
    }.toSet
  }

  private def getWebJarPaths: Def.Initialize[Task[Set[(String, String)]]] = Def.task {
    import scala.collection.JavaConverters._
    webjarCdn.value match {
      case Some(cdn) =>
        val locator = new WebJarAssetLocator(WebJarAssetLocator.getFullPathIndex(Pattern.compile(".*"), (webJarsClassLoader in Assets).value))
        locator.getWebJars.asScala.map {
          entry =>
            val (module, version) = entry
            s"$module" -> s"$cdn/$module/$version"
        }.toSet
      case _ => Set.empty
    }
  }

  private def runOptimizer: Def.Initialize[Task[Pipeline.Stage]] = Def.task {
    mappings =>

      val include = (includeFilter in rjs).value
      val exclude = (excludeFilter in rjs).value
      val optimizerMappings = mappings.filter(f => !f._1.isDirectory && include.accept(f._1) && !exclude.accept(f._1))
      SbtWeb.syncMappings(
        streams.value.cacheDirectory,
        optimizerMappings,
        appDir.value
      )

      val targetBuildProfileFile = (resourceManaged in rjs).value / "app.build.js"
      IO.write(targetBuildProfileFile, appBuildProfile.value, Utf8)

      val cacheDirectory = streams.value.cacheDirectory / rjs.key.label
      val runUpdate = FileFunction.cached(cacheDirectory, FilesInfo.hash) {
        _ =>
          streams.value.log("Optimizing JavaScript with RequireJS")

          SbtJsTask.executeJs(
            state.value,
            (engineType in rjs).value,
            (command in rjs).value,
            Nil,
            (webJarsNodeModulesDirectory in Plugin).value / "requirejs" / "bin" / "r.js",
            Seq("-o", targetBuildProfileFile.getAbsolutePath),
            (timeoutPerSource in rjs).value * optimizerMappings.size
          )

          appDir.value.***.get.toSet
      }

      val dirStr = dir.value.getAbsolutePath
      val optimizedMappings = runUpdate(Set(appDir.value)).filter(f => f.isFile && f.getAbsolutePath.startsWith(dirStr)).pair(relativeTo(dir.value))
      (mappings.toSet -- optimizerMappings.toSet ++ optimizedMappings).toSeq
  }
}
