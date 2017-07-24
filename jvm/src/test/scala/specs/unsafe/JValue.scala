package specs.unsafe

import specs.Spec
import scalajson.ast.unsafe._
import Generators._

class JValue extends Spec {
  def is =
    s2"""
  The JValue value should
   equals $testEquals
  """

  def testEquals = prop { jValue: scalajson.ast.unsafe.JValue =>
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
    jValue must beEqualTo(cloned)
  }
}
