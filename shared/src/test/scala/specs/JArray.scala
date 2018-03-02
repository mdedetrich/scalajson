package specs

import Generators._

/**
  * Created by matthewdedetrich on 5/05/2016.
  */
class JArray extends Spec {

  "The JArray value" should {
    "convert toUnsafe" in {
      forAll { jArray: scalajson.ast.JArray =>
        val values = jArray.value.map(_.toUnsafe).toArray

        Utils.unsafeJValueEquals(
          jArray.toUnsafe,
          scalajson.ast.unsafe.JArray(values)
        ) should be(true)
      }
    }

    "equals" in {
      forAll { jArray: scalajson.ast.JArray =>
        scalajson.ast.JArray(jArray.value) should be(
          scalajson.ast.JArray(jArray.value))
      }
    }
  }
}
