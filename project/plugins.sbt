resolvers ++= Seq(
  Resolver.typesafeRepo("releases"), 
  Resolver.sonatypeRepo("releases"), 
  Resolver.sonatypeRepo("snapshots"))

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")
addSbtPlugin("io.frees" % "sbt-freestyle" % "0.0.1-SNAPSHOT")
