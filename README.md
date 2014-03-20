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

The plugin is built on top of [JavaScript Engine](https://github.com/typesafehub/js-engine) which supports different JavaScript runtimes.

&copy; Typesafe Inc., 2014