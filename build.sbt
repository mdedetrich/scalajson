// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.crossProject
import Util.mkGitHubDeveloper

inScope(Scope.GlobalScope)(List(
  developers := List(
    mkGitHubDeveloper("mdedetrich", "Matthew de Detrich", "mdedetrich@gmail.com")
  ),
))

inThisBuild(List(
  scalaVersion := ScalaVersions.latest212,
  crossScalaVersions :=
    List(ScalaVersions.latest210, ScalaVersions.latest211, ScalaVersions.latest212),
  scalacOptions ++= Util.defaultScalacOptions,
))

lazy val root = makeRoot(project)
  .in(file("."))
  .aggregate(scalaJsonJS, scalaJsonJVM)

lazy val commonSettings = Seq(
  name := "scalajson",
  pomIncludeRepository := (_ => false),
  licenses += ("BSD 3 Clause", url(
    "https://opensource.org/licenses/BSD-3-Clause"))
)

lazy val scalaJson = crossProject(JSPlatform, JVMPlatform)
  .in(file("."))
  .settings(
    name := "scalajson",
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
    scalacOptions ++= {
      import Util.{flagsFor12, flagsFor11, flagsFor10}
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 12 => flagsFor12 :+ "-target:jvm-1.8"
        case Some((2, n)) if n == 11 => flagsFor11 :+ "-target:jvm-1.6"
        case Some((2, n)) if n == 10 => flagsFor10 :+ "-target:jvm-1.6"
        case v => sys.error(s"Unsupported Scala version $v")
      }
    },
  )
  .jvmSettings(
    // Add JVM-specific settings here
    libraryDependencies ++=
      Seq(Dependencies.specs2Core, Dependencies.specs2ScalaCheck, Dependencies.scalaCheck),
    scalacOptions in Test ++= Seq("-Yrangepos"),
    mimaPreviousArtifacts := Set("org.scala-lang.platform" %% "scalajson" % "1.0.0-M3")
  )
  .jsSettings(
    // Add JS-specific settings here
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "utest" % Dependencies.utestVersion % Test,
      "org.scalacheck" %%% "scalacheck" % Dependencies.scalaCheckVersion % Test
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

// In sbt 1.0.3, we will be able to make this the default name 'scalajson' by changing the id
lazy val scalaJsonJVM = scalaJson.jvm
lazy val scalaJsonJS = scalaJson.js

lazy val benchmark = crossProject(JSPlatform, JVMPlatform)
  .in(file("benchmark"))
  .dependsOn(scalaJson)
  .jvmSettings(
    testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
    libraryDependencies += Dependencies.scalameter,
  )

lazy val benchmarkJVM = benchmark.jvm
lazy val benchmarkJS = benchmark.js
