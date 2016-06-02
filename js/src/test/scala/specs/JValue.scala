package specs

import org.scalacheck.Prop._
import utest._

import scala.json.ast._
import Generators._

import scala.scalajs.js

object JValue extends TestSuite with UTestScalaCheck {

  val tests = TestSuite {
    "The JString value should" - {
      "equals" - testEquals
    }
  }

  def testEquals =
    forAll { jValue: scala.json.ast.JValue =>
      val cloned = jValue match {
        case scala.json.ast.JNull       => scala.json.ast.JNull
        case scala.json.ast.JTrue       => scala.json.ast.JTrue
        case scala.json.ast.JFalse      => scala.json.ast.JFalse
        case j: scala.json.ast.JNumber  => j.copy()
        case j: scala.json.ast.JString  => j.copy()
        case j: scala.json.ast.JArray   => j.copy()
        case j: scala.json.ast.JObject  => j.copy()
      }
      jValue == cloned
    }.checkUTest()
}
