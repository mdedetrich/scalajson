package specs.unsafe

import specs.Spec

import scalajson.ast.unsafe._
import Generators._

import scala.collection.immutable.ListMap

class JObject extends Spec {

  "The unsafe.JObject value" should {
    "convert toStandard" in {
      forAll { jObject: scalajson.ast.unsafe.JObject =>
        val values = jObject.value.map { x =>
          (x.field, x.value.toStandard)
        }.toMap
        jObject.toStandard == scalajson.ast.JObject(values)
      }
    }

    "convert toStandard with ListMap" in {
      forAll { jObject: scalajson.ast.unsafe.JObject =>
        val values = ListMap(
          jObject.value
            .map { x =>
              (x.field, x.value.toStandard)
            }
            .groupBy { case (k, _) => k }
            .map(_._2.head)
            .toList: _*)
        jObject.toStandard[ListMap] == scalajson.ast.JObject(values)
      }
    }

    "have a useful toString" in {
      "" + JObject(JField("a", JObject(JField("b", JFalse)))) ===
        "JObject([JField(a,JObject([JField(b,JFalse)]))])"
    }
  }

}
