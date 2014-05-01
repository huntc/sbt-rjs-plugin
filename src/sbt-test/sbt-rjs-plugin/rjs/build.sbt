lazy val root = (project in file(".")).addPlugins(SbtWeb)

pipelineStages := Seq(rjs)
