addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.16")
addSbtPlugin("org.scala-native" % "sbt-crossproject" % "0.2.0")
addSbtPlugin("org.scala-native" % "sbt-scalajs-crossproject" % "0.2.0")
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "0.6.8")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

// Ideal for local changes.
val PlatformBuild = RootProject(uri("git://github.com/scalacenter/platform.git#f4772bcffeaa76cfcf69c0d27a177367ab1602cc"))
dependsOn(ProjectRef(PlatformBuild.build, "sbt-platform"))
