name := "scalajson"

import PgpKeys.publishSigned

// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.crossProject

val currentScalaVersion = "2.11.12"
val scala210Version = "2.10.7"
val scala212Version = "2.12.4"
val scala213Version = "2.13.0-M3"
val scalaCheckVersion = "1.13.5"
val scalaTestVersion = "3.0.5-M1"  // only version avail for Scala 2.13.0-M3; otherwise same as 3.0.4

scalaVersion in ThisBuild := currentScalaVersion
crossScalaVersions in ThisBuild := Seq(currentScalaVersion,
                                       scala213Version,
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
  "-opt-inline-from:<sources>"
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

lazy val scalaJson = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .in(file("."))
  .settings(
    commonSettings,
    // In our build, implementations are specific due to use using sealed traits so a build defined
    // in scala-2.10 can't use the same sources as the generic 'scala' build. This removes the 'scala'
    // directory from sources when building for Scala 2.10.x
    (unmanagedSourceDirectories in Compile) := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 11 =>
          (unmanagedSourceDirectories in Compile).value
        case Some((2, n)) if n == 10 =>
          (unmanagedSourceDirectories in Compile).value.filter { x =>
            !x.getName.endsWith("scala")
          }
      }
    },
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
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "org.scalacheck" %% "scalacheck" % scalaCheckVersion % Test
    ),
    // scalacOptions in Test ++= Seq("-Yrangepos"), TODO Remove when https://github.com/scala/bug/issues/10706 is merged
    mimaPreviousArtifacts := Set(
      "org.scala-lang.platform" %% "scalajson" % "1.0.0-M3")
  )
  .jsSettings(
    // Add JS-specific settings here
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % scalaTestVersion % Test,
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % Test
    ),
    testFrameworks += TestFrameworks.ScalaTest
    // scalacOptions in Test ++= Seq("-Yrangepos") TODO Remove when https://github.com/scala/bug/issues/10706 is merged
  )
  .nativeSettings(
    crossScalaVersions := Seq(currentScalaVersion)
  )

lazy val scalameterVersion = settingKey[String]("version of ScalaMeter dependency")
scalameterVersion in ThisBuild := (CrossVersion.partialVersion(scalaVersion.value) match {
  // this snapshot is the only thing available for 2.13.0-M3 at present
  case Some ((2, n)) if n == 13 => "0.10-SNAPSHOT"
  case _ => "0.8.2"
})

lazy val benchmark = crossProject(JSPlatform, JVMPlatform)
  .in(file("benchmark"))
  .jvmSettings(
    testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
    // need snapshot resolver because ScalaMeter is only available for 2.13.0-M3 as a snapshot
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies ++= Seq(
      "com.storm-enroute" %% "scalameter" % scalameterVersion.value % Test
    )
  )
  .dependsOn(scalaJson)

lazy val scalaJsonJVMTest = benchmark.jvm
lazy val scalaJsonJSTest = benchmark.js

lazy val scalaJsonJVM = scalaJson.jvm
lazy val scalaJsonJS = scalaJson.js
lazy val scalaJsonNative = scalaJson.native
