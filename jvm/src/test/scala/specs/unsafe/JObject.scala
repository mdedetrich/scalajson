package specs.unsafe

import specs.Spec

import scalajson.ast.unsafe._
import Generators._

import scala.collection.immutable.VectorMap

class JObject extends Spec {
  def is =
    s2"""
  The unsafe.JArray value should
    convert toStandard $toStandard
  """

  def toStandard = prop { jObject: scalajson.ast.unsafe.JObject =>
    val values = jObject.value.map { x =>
      (x.field, x.value.toStandard)
    }.toMap

    val mapped = {
      val b = VectorMap.newBuilder[String, scalajson.ast.JValue]
      for (x <- values)
        b += x

      b.result()
    }

    jObject.toStandard == scalajson.ast.JObject(mapped)
  }

}
