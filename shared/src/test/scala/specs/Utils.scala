package specs

object Utils {

  /**
    * Since [[scalajson.ast.unsafe.JValue]] has mutable data-structures, it doesn't do structural equality
    * by default. Also takes care of the [[scalajson.unsafe.JObject]] special case with duplicate keys/ordering
    * @param left
    * @param right
    * @return
    */
  def unsafeJValueEquals(left: scalajson.ast.unsafe.JValue,
                         right: scalajson.ast.unsafe.JValue): Boolean = {
    (left, right) match {
      case (l: scalajson.ast.unsafe.JString, r: scalajson.ast.unsafe.JString) =>
        l == r
      case (scalajson.ast.unsafe.JNull, scalajson.ast.unsafe.JNull) =>
        true
      case (l: scalajson.ast.unsafe.JNumber, r: scalajson.ast.unsafe.JNumber) =>
        l == r
      case (l: scalajson.ast.unsafe.JArray, r: scalajson.ast.unsafe.JArray) =>
        val rValue = r.value
        l.value.zipWithIndex.forall {
          case (value, index) =>
            unsafeJValueEquals(value, rValue(index))
        }
      case (l: scalajson.ast.unsafe.JBoolean,
            r: scalajson.ast.unsafe.JBoolean) =>
        l == r
      case (l: scalajson.ast.unsafe.JObject, r: scalajson.ast.unsafe.JObject) =>
        val rAsMap = r.value.map { field =>
          (field.field, field.value)
        }.toMap
        val lAsMap = l.value.map { field =>
          (field.field, field.value)
        }.toMap
        rAsMap.forall {
          case (k, value) =>
            lAsMap.get(k) match {
              case Some(lValue) => unsafeJValueEquals(lValue, value)
              case _            => false
            }
        }

      case _ => false
    }
  }

}
