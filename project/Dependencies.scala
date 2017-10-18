import sbt.{Test, stringToOrganization}

object Dependencies {
  val utestVersion = "0.4.4"
  val specs2Version = "3.9.1"
  val scalaCheckVersion = "1.13.4"
  val scalameterVersion = "0.8.2"

  val specs2Core = "org.specs2" %% "specs2-core" % specs2Version % Test
  val specs2ScalaCheck = "org.specs2" %% "specs2-scalacheck" % specs2Version % Test
  val scalaCheck = "org.scalacheck" %% "scalacheck" % scalaCheckVersion % Test
  val scalameter = "com.storm-enroute" %% "scalameter" % scalameterVersion % Test
}
