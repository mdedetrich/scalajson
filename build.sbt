name := "Scala Json AST"

val currentScalaVersion = "2.11.8"
val scala210Version = "2.10.6"
val scalaCheckVersion = "1.13.0"
val specs2Version = "3.7.2"

scalaVersion in ThisBuild := currentScalaVersion
crossScalaVersions in ThisBuild := Seq(currentScalaVersion, scala210Version)

lazy val root = project.in(file(".")).
  aggregate(scalaJsonASTJS, scalaJsonASTJVM).
  settings(
    publish := {},
    publishLocal := {}
  )

lazy val scalaJsonAST = crossProject.in(file(".")).
  settings(
    name := "scala-json-ast",
    version := "1.0.0-M2",
    organization := "org.mdedetrich",
    scalacOptions ++= Seq(
      "-encoding", "UTF-8",
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
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra := <url>https://github.com/mdedetrich/scala-json-ast</url>
      <licenses>
        <license>
          <name>BSD 3-Clause</name>
          <url>https://opensource.org/licenses/BSD-3-Clause</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:mdedetrich/scala-json-ast.git</url>
        <connection>scm:git:git@github.com:mdedetrich/scala-json-ast.git</connection>
      </scm>
      <developers>
        <developer>
          <id>mdedetrich</id>
          <name>Matthew de Detrich</name>
          <email>mdedetrich@gmail.com</email>
        </developer>
      </developers>,
    scalacOptions += {
      scalaVersion.value match {
        case v if v.startsWith("2.10.") => "-target:jvm-1.6"
        case v if v.startsWith("2.11.") => "-target:jvm-1.6"
        case v if v.startsWith("2.12.") => "-target:jvm-1.8"
      }
    }
  ).
  jvmSettings(
    // Add JVM-specific settings here
    testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
    libraryDependencies ++= Seq(
      "com.storm-enroute" %% "scalameter" % "0.7" % Test,
      "org.specs2" %% "specs2-core" % specs2Version % Test,
      "org.specs2" %% "specs2-scalacheck" % specs2Version % Test,
      "org.scalacheck" %% "scalacheck" % scalaCheckVersion % Test
    ),
    scalacOptions in Test ++= Seq("-Yrangepos")
  ).
  jsSettings(
    // Add JS-specific settings here
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % Test,
      "com.lihaoyi" %%% "utest" % "0.4.3" % Test
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val scalaJsonASTJVM = scalaJsonAST.jvm
lazy val scalaJsonASTJS = scalaJsonAST.js
