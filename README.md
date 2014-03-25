sbt-rjs-plugin
==============

[![Build Status](https://api.travis-ci.org/sbt/sbt-rjs-plugin.png?branch=master)](https://travis-ci.org/sbt/sbt-rjs-plugin)

An SBT plugin to perform [RequireJs optimization](http://requirejs.org/docs/optimization.html).

To use this plugin use the addSbtPlugin command within your project's `plugins.sbt` file:

    resolvers ++= Seq(
        Resolver.url("sbt snapshot plugins", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
        Resolver.sonatypeRepo("snapshots"),
        "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
        )

    addSbtPlugin("com.typesafe.sbt" % "sbt-rjs-plugin" % "1.0.0-SNAPSHOT")

WebJars are treated specially. If a path is referenced that is part of a path belong to a Webjar then the `webjarCdn`
setting is used to translate it to the CDN. This is all fully automatic and provided as part of a [buildWriter](http://www.ericfeminella.com/blog/2012/03/24/preprocessing-modules-with-requirejs-optimizer/)
function. Furthermore if a `.bin` or `-bin` equivalent of the resource is available then it is used. The end result is
that all WebJar sourced resources are located via a CDN along with their minified versions.

A standard build profile for the RequireJS optimizer is provided. However if you would prefer to provide your own
build profile then create an `app.build.js` file in your project's folder. For more information on build profiles
see http://requirejs.org/docs/optimization.html. Note that one requirement for these build profiles is to accept the
last line being a line to receive four parameters passed by this plugin. Whether you use them or not is at your
discretion. Here is the default app.build.js profile which you should use as a basis for your own:

```javascript
(function (appDir, dir, paths, buildWriter) {
    return {
        appDir: appDir,
        baseUrl: "js",
        dir: dir,
        generateSourceMaps: true,
        mainConfigFile: appDir + "/js/main.js",
        modules: [
            {
                name: "main"
            }
        ],
        onBuildWrite: buildWriter,
        optimize: "uglify2",
        paths: paths,
        preserveLicenseComments: false
    }
}(undefined, undefined, undefined, undefined))
```

The plugin is built on top of [JavaScript Engine](https://github.com/typesafehub/js-engine) which supports different JavaScript runtimes.

&copy; Typesafe Inc., 2014