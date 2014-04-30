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

  object RjsKeys {
    val rjs = TaskKey[Pipeline.Stage]("rjs", "Perform RequireJs optimization on the asset pipeline.")

    val baseUrl = SettingKey[Option[String]]("rjs-baseUrl", """The dir relative to the assets folder where js files are housed. Will default to "js", "javascripts" or "." with the latter if the other two cannot be found.""")
    val paths = TaskKey[Set[(String, String)]]("rjs-paths", "A set of RequireJS path mappings. By default all WebJar libraries are made available from a CDN and their mappings can be found here (unless the cdn is set to None).")
    val projectBuildProfile = SettingKey[File]("rjs-project-profile", "The project build profile file. If it doesn't exist then a default one will be used.")
    val webjarCdn = SettingKey[Option[String]]("rjs-webjar-cdn", "A CDN to be used for locating WebJars. By default jsdelivr is used.")
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
  import autoImport.RjsKeys._

  override def projectSettings = Seq(
    baseUrl := None,
    excludeFilter in rjs := HiddenFileFilter,
    includeFilter in rjs := GlobFilter("*.js") | GlobFilter("*.css") | GlobFilter("*.map"),
    paths := getWebJarPaths.value,
    projectBuildProfile := baseDirectory.value / "app.build.js",
    resourceManaged in rjs := webTarget.value / rjs.key.label,
    rjs := runOptimizer.dependsOn(webJarsNodeModules in Plugin).value,
    pipelineStages <+= rjs,
    webjarCdn := Some("http://cdn.jsdelivr.net/webjars")
  )


  val Utf8 = Charset.forName("UTF-8")

  private def getResourceAsList(name: String): List[String] = {
    val in = SbtRjs.getClass.getClassLoader.getResourceAsStream(name)
    val reader = new BufferedReader(new InputStreamReader(in, Utf8))
    try {
      IO.readLines(reader)
    } finally {
      reader.close()
    }
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

      val appDir = (resourceManaged in rjs).value / "appdir"
      val dir = appDir / "build"


      val include = (includeFilter in rjs).value
      val exclude = (excludeFilter in rjs).value
      val optimizerMappings = mappings.filter(f => !f._1.isDirectory && include.accept(f._1) && !exclude.accept(f._1))
      SbtWeb.syncMappings(
        streams.value.cacheDirectory,
        optimizerMappings,
        appDir
      )


      val templateBuildProfileContents =
        if (projectBuildProfile.value.exists()) {
          IO.readLines(projectBuildProfile.value, Utf8)
        } else {
          getResourceAsList("template.build.js")
        }


      val DotJS = ".js"
      val webJarModuleIds = (webJars in Assets).value.collect {
        case f if f.name.endsWith(DotJS) => f.name.dropRight(DotJS.length)
      }.toSet

      val buildWriter = getResourceAsList("buildWriter.js")
        .to[Vector]
        .dropRight(1) :+ s"""})(
          "${webModulesLib.value}/",
          ${toJsonObj(paths.value)},
          ${toJsonObj(webJarModuleIds.map(m => m -> m))}
          )"""


      val resolvedBaseUrl = baseUrl.value.getOrElse {
        if ((appDir / "js").exists) {
          "js"
        } else if ((appDir / "javascripts").exists) {
          "javascripts"
        } else {
          "."
        }
      }
      val appBuildProfileContents = templateBuildProfileContents
        .to[Vector]
        .dropRight(1) :+ s"""}(
          "${appDir.getAbsolutePath}",
          "$resolvedBaseUrl",
          "${dir.getAbsolutePath}",
          ${toJsonObj(webJarModuleIds.map(m => m -> "empty:"))},
          ${buildWriter.mkString("\n")}
        )) """
      val appBuildProfile = (resourceManaged in rjs).value / "app.build.js"
      IO.writeLines(appBuildProfile, appBuildProfileContents, Utf8)


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
            Seq("-o", appBuildProfile.getAbsolutePath),
            (timeoutPerSource in rjs).value * optimizerMappings.size
          )

          appDir.***.get.toSet
      }

      val dirStr = dir.getAbsolutePath
      val optimizedMappings = runUpdate(Set(appDir)).filter(f => f.isFile && f.getAbsolutePath.startsWith(dirStr)).pair(relativeTo(dir))
      (mappings.toSet -- optimizerMappings.toSet ++ optimizedMappings).toSeq
  }


  private def toJsonObj(entries: Set[(String, String)]): String = entries.map {
    case (key, value) => s""""$key":"$value" """
  }.mkString("{", ",", "}")


}
