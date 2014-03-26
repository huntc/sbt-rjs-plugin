import com.typesafe.sbt.web.SbtWebPlugin

lazy val root = project.in(file(".")).addPlugins(SbtWebPlugin)