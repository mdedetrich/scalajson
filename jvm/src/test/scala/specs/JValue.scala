package specs

import Generators._

class JValue extends Spec {
  def is =
    s2"""
  The JArray value should
   equals $testEquals
  """

  def testEquals = prop { jValue: scalajson.ast.JValue =>
    // Is there a better way to do this?
    val cloned = jValue match {
      case scalajson.ast.JNull => scalajson.ast.JNull
      case jNumber: scalajson.ast.JNumber =>
        scalajson.ast.JNumber.fromString(jNumber.stringValue).get
      case jString: scalajson.ast.JString =>
        scalajson.ast.JString(jString.value)
      case jArray: scalajson.ast.JArray => scalajson.ast.JArray(jArray.value)
      case jObject: scalajson.ast.JObject =>
        scalajson.ast.JObject(jObject.value)
      case jBoolean: scalajson.ast.JBoolean =>
        scalajson.ast.JBoolean(jBoolean.get)
    }
    jValue must beEqualTo(cloned)
  }
}
