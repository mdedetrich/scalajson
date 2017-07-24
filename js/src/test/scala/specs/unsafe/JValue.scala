package specs.unsafe

import org.scalacheck.Prop._
import specs.UTestScalaCheck
import utest._

import Generators._

object JValue extends TestSuite with UTestScalaCheck {
  val tests = TestSuite {
    "The JValue value should" - {
      "equals" - testEquals
    }
  }

  def testEquals =
    forAll { jValue: scalajson.ast.unsafe.JValue =>
      // Is there a better way to do this?
      val cloned = jValue match {
        case scalajson.ast.unsafe.JNull => scalajson.ast.unsafe.JNull
        case jNumber: scalajson.ast.unsafe.JNumber =>
          scalajson.ast.unsafe.JNumber(jNumber.stringValue).get
        case jString: scalajson.ast.unsafe.JString =>
          scalajson.ast.unsafe.JString(jString.value)
        case jArray: scalajson.ast.unsafe.JArray =>
          scalajson.ast.unsafe.JArray(jArray.value)
        case jObject: scalajson.ast.unsafe.JObject =>
          scalajson.ast.unsafe.JObject(jObject.value)
        case jBoolean: scalajson.ast.unsafe.JBoolean =>
          scalajson.ast.unsafe.JBoolean(jBoolean.get)
      }
      jValue == cloned
    }.checkUTest()
}
