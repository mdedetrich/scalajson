package specs

import Generators._

class JObject extends Spec {
  "The JObject value" should {
    "convert toUnsafe" in {
      forAll { jObject: scalajson.ast.JObject =>
        val values = jObject.value.map {
          case (k, v) =>
            scalajson.ast.unsafe.JField(k, v.toUnsafe)
        }
        Utils.unsafeJValueEquals(jObject.toUnsafe,
                                 scalajson.ast.unsafe.JObject(values.toArray))
      }
    }

    "equals" in {
      forAll { jObject: scalajson.ast.JObject =>
        scalajson.ast.JObject(jObject.value) should be(
          scalajson.ast.JObject(jObject.value))
      }
    }
  }
}
