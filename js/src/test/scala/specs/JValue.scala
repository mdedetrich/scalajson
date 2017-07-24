package specs

import org.scalacheck.Prop._
import utest._

import scalajson.ast._
import Generators._

import scala.scalajs.js

object JValue extends TestSuite with UTestScalaCheck {

  val tests = TestSuite {
    "The JString value should" - {
      "equals" - testEquals
    }
  }

  def testEquals =
    forAll { jValue: scalajson.ast.JValue =>
      // Is there a better way to do this?
      val cloned = jValue match {
        case scalajson.ast.JNull => scalajson.ast.JNull
        case jNumber: scalajson.ast.JNumber =>
          scalajson.ast.JNumber.fromString(jNumber.stringValue).get
        case jString: scalajson.ast.JString =>
          scalajson.ast.JString(jString.value)
        case jArray: scalajson.ast.JArray =>
          scalajson.ast.JArray(jArray.value)
        case jObject: scalajson.ast.JObject =>
          scalajson.ast.JObject(jObject.value)
        case jBoolean: scalajson.ast.JBoolean =>
          scalajson.ast.JBoolean(jBoolean.get)
      }
      jValue == cloned
    }.checkUTest()
}
