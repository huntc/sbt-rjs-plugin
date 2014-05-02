lazy val root = (project in file(".")).enablePlugins(SbtWeb)

pipelineStages := Seq(rjs)
