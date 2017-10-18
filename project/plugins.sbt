addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.20")
addSbtPlugin("org.scala-native" % "sbt-crossproject" % "0.2.2")
addSbtPlugin("org.scala-native" % "sbt-scalajs-crossproject" % "0.2.2")
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.3.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

// This is temporary
resolvers += "Jenkins repo" at "http://repo.jenkins-ci.org/public/"

// Ideal for local changes.
val PlatformBuild = RootProject(uri("git://github.com/scalacenter/platform.git#f4772bcffeaa76cfcf69c0d27a177367ab1602cc"))
dependsOn(ProjectRef(PlatformBuild.build, "sbt-platform"))
