package com.typesafe.sbt.rjs

import sbt._
import sbt.Keys._
import com.typesafe.sbt.web.SbtWebPlugin
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.jse.{SbtJsEnginePlugin, SbtJsTaskPlugin}
import java.nio.charset.Charset
import java.io.{BufferedReader, InputStreamReader}

object SbtRjsPlugin extends AutoPlugin {

  def select = SbtJsTaskPlugin

  object RjsKeys {
    val rjs = TaskKey[Pipeline.Stage]("rjs", "Perform RequireJs optimization on the asset pipeline.")

    val projectBuildProfile = SettingKey[File]("rjs-project-profile", "The project build profile file. If it doesn't exist then a default one will be used.")
  }


  import SbtWebPlugin._
  import SbtWebPlugin.WebKeys._
  import SbtJsEnginePlugin.JsEngineKeys._
  import SbtJsTaskPlugin.JsTaskKeys._
  import RjsKeys._

  override def projectSettings = Seq(
    excludeFilter in rjs := HiddenFileFilter,
    includeFilter in rjs := GlobFilter("*.js") | GlobFilter("*.css"),
    projectBuildProfile := baseDirectory.value / "app.build.js",
    resourceManaged in rjs := webTarget.value / rjs.key.label,
    rjs := runOptimizer.dependsOn(webJarsNodeModules in Plugin).value,
    stages <+= rjs
  )


  val Utf8 = Charset.forName("UTF-8")

  def runOptimizer: Def.Initialize[Task[Pipeline.Stage]] = Def.task {
    mappings =>

      val templateBuildProfileContents =
        if (projectBuildProfile.value.exists()) {
          IO.readLines(projectBuildProfile.value, Utf8)
        } else {
          val in = SbtRjsPlugin.getClass.getClassLoader.getResourceAsStream("template.build.js")
          val reader = new BufferedReader(new InputStreamReader(in, Utf8))
          try {
            IO.readLines(reader)
          } finally {
            reader.close()
          }
        }


      val appDir = (resourceManaged in rjs).value / "appdir"
      val dir = appDir / "build"


      val appBuildProfileContents = templateBuildProfileContents
        .to[Vector]
        .dropRight(1) :+ s"""}("${appDir.getAbsolutePath}", "${dir.getAbsolutePath}"))"""
      val appBuildProfile = (resourceManaged in rjs).value / "app.build.js"
      IO.writeLines(appBuildProfile, appBuildProfileContents, Utf8)


      val include = (includeFilter in rjs).value
      val exclude = (excludeFilter in rjs).value
      val optimizerMappings = mappings.filter(f => include.accept(f._1) && !exclude.accept(f._1))
      syncMappings(
        streams.value.cacheDirectory,
        optimizerMappings,
        appDir
      )


      val cacheDirectory = streams.value.cacheDirectory / rjs.key.label
      val runUpdate = FileFunction.cached(cacheDirectory, FilesInfo.hash) {
        _ =>
          streams.value.log("Optimizing JavaScript with RequireJS")

          SbtJsTaskPlugin.executeJs(
            state.value,
            (engineType in rjs).value,
            Nil,
            (webJarsNodeModulesDirectory in Plugin).value / "requirejs" / "bin" / "r.js",
            Seq("-o", appBuildProfile.getAbsolutePath),
            (timeoutPerSource in rjs).value * optimizerMappings.size
          )

          appDir.***.get.toSet
      }

      val optimizedMappings = runUpdate(Set(appDir)).filter(f => f.getParentFile == dir).pair(relativeTo(dir))
      (mappings.toSet -- optimizerMappings.toSet ++ optimizedMappings).toSeq
  }

}
