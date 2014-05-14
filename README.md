sbt-rjs
=======

[![Build Status](https://api.travis-ci.org/sbt/sbt-rjs.png?branch=master)](https://travis-ci.org/sbt/sbt-rjs)

An SBT plugin to perform [RequireJs optimization](http://requirejs.org/docs/optimization.html).

To use this plugin use the addSbtPlugin command within your project's `plugins.sbt` file:

    addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.0-RC1")

Your project's build file also needs to enable sbt-web plugins. For example with build.sbt:

    lazy val root = (project in file(".")).enablePlugins(SbtWeb)

As with all sbt-web asset pipeline plugins you must declare their order of execution e.g.:

```scala
pipelineStages := Seq(rjs)
```

WebJars are treated specially. If a path is referenced that is part of a path belong to a Webjar then the `webjarCdn`
setting is used to translate it to the CDN. This is all fully automatic and provided as part of a [buildWriter](http://www.ericfeminella.com/blog/2012/03/24/preprocessing-modules-with-requirejs-optimizer/)
function. Furthermore if a `.bin` or `-bin` equivalent of the resource is available then it is used. The end result is
that all WebJar sourced resources are located via a CDN along with their minified versions.

RequireJs optimization [permits build profiles](http://requirejs.org/docs/optimization.html#basics)
to be declared that specify what needs to be done. A standard build profile for the RequireJS optimizer is provided.
However if you need to provide your own build profile then declare an `appBuildProfile` function in your build.
The following build profile is the direct equivalent of
[the one recommended in the rjs documentation for whole project builds](http://requirejs.org/docs/optimization.html#wholeproject):

```scala
import RjsKeys._

appBuildProfile := s"""|({
                       |  appDir: "${appDir.value}",
                       |  baseUrl: "js",
                       |  dir: "${dir.value}",
                       |  modules: [
                       |      {
                       |           name: "main"
                       |      }
                       |  ]
                       |})""".stripMargin
```

The standard build profile we provide incorporates support for generating source maps, allows for configuration overrides in your
`main.js` file and optimizes using uglify2. The build profile is as follows:

```scala
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
```

The plugin is built on top of [JavaScript Engine](https://github.com/typesafehub/js-engine) which supports different JavaScript runtimes.

&copy; Typesafe Inc., 2014
