name := "scalajson"

import PgpKeys.publishSigned

// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.crossProject

val currentScalaVersion = "2.11.11"
val scala210Version = "2.10.6"
val scala212Version = "2.12.2"
val scalaCheckVersion = "1.13.4"
val specs2Version = "3.9.1"

scalaVersion in ThisBuild := currentScalaVersion
crossScalaVersions in ThisBuild := Seq(currentScalaVersion,
                                       scala212Version,
                                       scala210Version)

autoAPIMappings := true

val flagsFor10 = Seq(
  "-Xlint",
  "-Yclosure-elim",
  "-Ydead-code"
)

val flagsFor11 = Seq(
  "-Xlint:_",
  "-Yconst-opt",
  "-Ywarn-infer-any",
  "-Yclosure-elim",
  "-Ydead-code",
  "-Xsource:2.12" // required to build case class construction
)

val flagsFor12 = Seq(
  "-Xlint:_",
  "-Ywarn-infer-any",
  "-opt:l:project"
)

lazy val root = project
  .in(file("."))
  .aggregate(scalaJsonJS, scalaJsonJVM)
  .settings(
    publish := {},
    publishLocal := {},
    publishSigned := {}
  )

lazy val commonSettings = Seq(
  name := "scalajson",
  version := "1.0.0-M4",
  organization := "org.scala-lang.platform",
  scalacOptions ++= Seq(
    "-encoding",
    "UTF-8",
    "-deprecation", // warning and location for usages of deprecated APIs
    "-feature", // warning and location for usages of features that should be imported explicitly
    "-unchecked", // additional warnings where generated code depends on assumptions
    "-Xlint", // recommended additional warnings
    "-Xcheckinit", // runtime error when a val is not initialized due to trait hierarchies (instead of NPE somewhere else)
    "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
    "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
    "-Ywarn-inaccessible",
    "-Ywarn-dead-code"
  ),
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  pomIncludeRepository := (_ => false),
  homepage := Some(url("https://github.com/mdedetrich/scalajson")),
  scmInfo := Some(
    ScmInfo(url("https://github.com/mdedetrich/scalajson"),
            "git@github.com:mdedetrich/scalajson.git")),
  developers := List(
    Developer("mdedetrich",
              "Matthew de Detrich",
              "mdedetrich@gmail.com",
              url("https://github.com/mdedetrich"))
  ),
  licenses += ("BSD 3 Clause", url(
    "https://opensource.org/licenses/BSD-3-Clause"))
)

lazy val scalaJson = crossProject(JSPlatform, JVMPlatform)
  .in(file("."))
  .settings(
    commonSettings,
    scalacOptions += {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 12 =>
          "-target:jvm-1.8"
        case _ =>
          "-target:jvm-1.6"
      }
    },
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 12 =>
          flagsFor12
        case Some((2, n)) if n == 11 =>
          flagsFor11
        case Some((2, n)) if n == 10 =>
          flagsFor10
      }
    }
  )
  .jvmSettings(
    // Add JVM-specific settings here
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2-core" % specs2Version % Test,
      "org.specs2" %% "specs2-scalacheck" % specs2Version % Test,
      "org.scalacheck" %% "scalacheck" % scalaCheckVersion % Test
    ),
    scalacOptions in Test ++= Seq("-Yrangepos"),
    mimaPreviousArtifacts := Set(
      "org.scala-lang.platform" %% "scalajson" % "1.0.0-M3")
  )
  .jsSettings(
    // Add JS-specific settings here
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % Test,
      "com.lihaoyi" %%% "utest" % "0.4.4" % Test
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val benchmark = crossProject(JSPlatform, JVMPlatform)
  .in(file("benchmark"))
  .jvmSettings(
    testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
    libraryDependencies ++= Seq(
      "com.storm-enroute" %% "scalameter" % "0.8.2" % Test
    )
  )
  .dependsOn(scalaJson)

lazy val scalaJsonJVMTest = benchmark.jvm
lazy val scalaJsonJSTest = benchmark.js

lazy val scalaJsonJVM = scalaJson.jvm
lazy val scalaJsonJS = scalaJson.js
