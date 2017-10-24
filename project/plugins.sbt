addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.20")
addSbtPlugin("org.scala-native" % "sbt-crossproject" % "0.2.2")
addSbtPlugin("org.scala-native" % "sbt-scalajs-crossproject" % "0.2.2")
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.3.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

// Ideal for local changes.
resolvers += "Jenkins repo" at "http://repo.jenkins-ci.org/public/"
val PlatformBuild = RootProject(uri("git://github.com/scalacenter/platform.git#50f7bff24e3083f5b726a5a954d53372429f0857"))
dependsOn(ProjectRef(PlatformBuild.build, "sbt-platform"))
