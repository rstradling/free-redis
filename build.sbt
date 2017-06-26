import sbt._
import Keys._


val freeStyleVersion = "0.3.0"

addCompilerPlugin("org.scalameta" %% "paradise" % "3.0.0-M9" cross CrossVersion.full)

val sharedSettings = Seq(
  libraryDependencies ++= Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "org.typelevel"  %% "cats" % "0.9.0",
    "org.scalactic" %% "scalactic" % "3.0.0",
    "org.scalatest" %% "scalatest" % "3.0.0" % "test",
    "io.frees" %% "freestyle" % freeStyleVersion
  ),
  resolvers += Resolver.typesafeRepo("releases")
)

logBuffered in Test := false

crossScalaVersions in ThisBuild := Seq("2.11.11", "2.12.2")

scalaVersion in ThisBuild := "2.12.2"

lazy val root = project
  .in(file("."))
  .settings(
  name := "free-redis",
    moduleName := "free-redis",
    mainClass in (Compile, run) := Some("com.strad.free.redis.client.Main")
)
  .aggregate(parser, command, client)
  .dependsOn(parser, command, client)

lazy val parser = project
  .settings(
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "fastparse" % "0.4.3"
  ),
    name := "parser",
    moduleName := "parser")
  .settings(sharedSettings)

lazy val command = project
  .settings(
  name := "command",
    moduleName := "command")
  .settings(sharedSettings)
  .dependsOn(parser)

lazy val client = project
  .settings(
  name := "client",
    moduleName := "client")
  .settings(sharedSettings)
  .dependsOn(parser, command)
