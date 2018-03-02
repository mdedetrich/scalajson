package specs

import Generators._

class JValue extends Spec {
  "The JValue value" should {
    "equals" in {
      forAll { jValue: scalajson.ast.JValue =>
        val cloned = jValue match {
          case scalajson.ast.JNull => scalajson.ast.JNull
          case jNumber: scalajson.ast.JNumber =>
            scalajson.ast.JNumber.fromString(jNumber.value).get
          case jString: scalajson.ast.JString =>
            scalajson.ast.JString(jString.value)
          case jArray: scalajson.ast.JArray =>
            scalajson.ast.JArray(jArray.value)
          case jObject: scalajson.ast.JObject =>
            scalajson.ast.JObject(jObject.value)
          case jBoolean: scalajson.ast.JBoolean =>
            scalajson.ast.JBoolean(jBoolean.get)
        }
        jValue should be(cloned)
      }
    }
  }
}
