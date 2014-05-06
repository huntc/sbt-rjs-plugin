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

By default this plugin assumes "js" or "javascripts" as the directory where JavaScript files are housed, defaulting to "." if neither
can be found. This may be quickly overridden by using the following configuration:

```scala
RjsKeys.baseUrl := Some("myjs")
```

A standard build profile for the RequireJS optimizer is provided. However if you would prefer to provide your own
build profile then create an `app.build.js` file in your project's folder. For more information on build profiles
see http://requirejs.org/docs/optimization.html. Note that one requirement for these build profiles is to accept the
last line being a line to receive five parameters passed by this plugin. Whether you use them or not is at your
discretion, but that last line must be there.

Here is the default app.build.js profile which you should use as a basis for any of your own:

```javascript
(function (appDir, baseUrl, dir, paths, buildWriter) {
    return {
        appDir: appDir,
        baseUrl: baseUrl,
        dir: dir,
        generateSourceMaps: true,
        mainConfigFile: appDir + "/" + baseUrl + "/main.js",
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
}(undefined, undefined, undefined, undefined, undefined))
```

The plugin is built on top of [JavaScript Engine](https://github.com/typesafehub/js-engine) which supports different JavaScript runtimes.

&copy; Typesafe Inc., 2014
