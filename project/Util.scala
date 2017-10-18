import sbt.{Developer, url}

object Util {
  val defaultScalacOptions = List(
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
  )

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

  def mkGitHubDeveloper(handle: String, fullName: String, email: String): Developer =
    Developer(handle, fullName, email, url(s"https://github.com/handle"))
}
