package scalajson.ast
package unsafe

import scalajson.ast
import scalajson.ast._
import scala.scalajs.js

/** Represents a JSON Value which may be invalid. Internally uses mutable
  * collections when its desirable to do so, for performance and other reasons
  * (such as ordering and duplicate keys)
  *
  * @author Matthew de Detrich
  * @see https://www.ietf.org/rfc/rfc4627.txt
  */
sealed abstract class JValue extends Serializable with Product {

  /**
    * Converts a [[unsafe.JValue]] to a [[ast.JValue]]. Note that
    * when converting [[unsafe.JNumber]], this can throw runtime error if the underlying
    * string representation is not a correct number. Also when converting a [[ast.JObject]]
    * to a [[ast.JObject]], its possible to lose data if you have duplicate keys.
    *
    * @see https://www.ietf.org/rfc/rfc4627.txt
    * @return
    */
  def toStandard: ast.JValue

  /**
    * Converts a [[unsafe.JValue]] to a Javascript object/value that can be used within
    * Javascript
    *
    * @return
    */
  def toJsAny: js.Any
}

/** Represents a JSON null value
  *
  * @author Matthew de Detrich
  */
final case object JNull extends JValue {
  override def toStandard: ast.JValue = ast.JNull

  override def toJsAny: js.Any = null
}

/** Represents a JSON string value
  *
  * @author Matthew de Detrich
  */
final case class JString(value: String) extends JValue {
  override def toStandard: ast.JValue = ast.JString(value)

  override def toJsAny: js.Any = value
}

object JNumber {
  def apply(value: Int): JNumber =
    JNumber(value.toString, NumberFlags.intConstructed)

  def apply(value: Long): JNumber =
    JNumber(value.toString, NumberFlags.longConstructed)

  def apply(value: BigInt): JNumber =
    JNumber(value.toString, NumberFlags.bigIntConstructed)

  def apply(value: BigDecimal): JNumber =
    JNumber(value.toString, NumberFlags.bigDecimalConstructed)

  def apply(value: Float): JNumber =
    JNumber(value.toString, NumberFlags.floatConstructed)

  def apply(value: Double): JNumber =
    JNumber(value.toString, NumberFlags.doubleConstructed)

  def apply(value: Integer): JNumber =
    JNumber(value.toString, NumberFlags.intConstructed)

  def apply(value: Array[Char]): JNumber = JNumber(new String(value), (0))
}

/** Represents a JSON number value.
  *
  * If you are passing in a NaN or Infinity as a [[scala.Double]], [[unsafe.JNumber]]
  * will contain "NaN" or "Infinity" as a String which means it will cause
  * issues for users when they use the value at runtime. It may be
  * preferable to check values yourself when constructing [[unsafe.JValue]]
  * to prevent this. This isn't checked by default for performance reasons.
  *
  * @author Matthew de Detrich
  */
// JNumber is internally represented as a string, to improve performance
final case class JNumber(value: String, constructedFlag: Int = 0)
    extends JValue {
  override def toStandard: ast.JValue =
    value match {
      case jNumberRegex(_ *) => new ast.JNumber(value)(constructedFlag)
      case _ => throw new NumberFormatException(value)
    }

  def this(value: Double) = {
    this(value.toString)
  }

  override def toJsAny: js.Any = value.toDouble match {
    case n if n.isNaN => null
    case n if n.isInfinity => null
    case n => n
  }

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case jNumber: JNumber => jNumber.value == this.value
      case _ => false
    }
  }

  override def hashCode(): Int = value.##

  def toInt: Option[Long] = {
    if ((constructedFlag & NumberFlags.int) == NumberFlags.int)
      Some(value.toInt)
    else {
      try {
        val asInt = value.toInt
        if (BigInt(value) == BigInt(asInt))
          Some(asInt)
        else
          None
      } catch {
        case _: NumberFormatException => None
      }
    }
  }

  def toLong: Option[Long] = {
    if ((constructedFlag & NumberFlags.long) == NumberFlags.long)
      Some(value.toLong)
    else {
      try {
        val asLong = value.toLong
        if (BigInt(value) == BigInt(asLong))
          Some(asLong)
        else
          None
      } catch {
        case _: NumberFormatException => None
      }
    }
  }

  def toBigInt: Option[BigInt] = {
    if ((constructedFlag & NumberFlags.bigInt) == NumberFlags.bigInt)
      Some(BigInt(value))
    else {
      try {
        Some(BigInt(value))
      } catch {
        case _: NumberFormatException => None
      }
    }
  }

  def toBigDecimal: Option[BigDecimal] = {
    try {
      Some(BigDecimal(value))
    } catch {
      case _: NumberFormatException => None
    }
  }

  def toFloat: Option[Float] = {
    if ((constructedFlag & NumberFlags.float) == NumberFlags.float)
      Some(value.toFloat)
    else {
      try {
        val asFloat = value.toFloat
        if (BigDecimal(value) == BigDecimal(asFloat.toDouble))
          Some(asFloat)
        else
          None
      } catch {
        case _: NumberFormatException => None
      }
    }
  }

  def toDouble: Option[Double] = {
    if ((constructedFlag & NumberFlags.double) == NumberFlags.double)
      Some(value.toDouble)
    else {
      try {
        val asDouble = value.toDouble
        if (BigDecimal(value) == BigDecimal(asDouble))
          Some(asDouble)
        else
          None
      } catch {
        case _: NumberFormatException => None
      }
    }
  }
}

/** Represents a JSON Boolean value, which can either be a
  * [[JTrue]] or a [[JFalse]]
  *
  * @author Matthew de Detrich
  */
// Implements named extractors so we can avoid boxing
sealed abstract class JBoolean extends JValue {
  def get: Boolean

  override def toJsAny: js.Any = get
}

object JBoolean {
  def apply(x: Boolean): JBoolean = if (x) JTrue else JFalse

  def unapply(x: JBoolean): Some[Boolean] = Some(x.get)
}

/** Represents a JSON Boolean true value
  *
  * @author Matthew de Detrich
  */
final case object JTrue extends JBoolean {
  override def get = true

  override def toStandard: ast.JValue = ast.JTrue
}

/** Represents a JSON Boolean false value
  *
  * @author Matthew de Detrich
  */
final case object JFalse extends JBoolean {
  override def get = false

  override def toStandard: ast.JValue = ast.JFalse
}

final case class JField(field: String, value: JValue)

object JObject {
  import js.JSConverters._
  def apply(value: JField, values: JField*): JObject =
    JObject(js.Array(value) ++ values.toJSArray)

  def apply(value: Array[JField]): JObject = JObject(value.toJSArray)
}

/** Represents a JSON Object value. Duplicate keys
  * are allowed and ordering is respected
  * @author Matthew de Detrich
  */
// JObject is internally represented as a mutable Array, to improve sequential performance
final case class JObject(value: js.Array[JField] = js.Array()) extends JValue {
  def this(value: js.Dictionary[JValue]) = {
    this({
      val array: js.Array[JField] = new js.Array()
      for (key <- value.keys) {
        array.push(JField(key, value(key)))
      }
      array
    })
  }

  override def toStandard: ast.JValue = {
    // Javascript array.length across all major browsers has near constant cost, so we
    // use this to build the array http://jsperf.com/length-comparisons
    val length = value.length

    if (length == 0) {
      ast.JObject(Map.newBuilder[String, ast.JValue].result())
    } else {
      val b = Map.newBuilder[String, ast.JValue]
      var index = 0
      while (index < length) {
        val v = value(index)
        b += ((v.field, v.value.toStandard))
        index += 1
      }
      ast.JObject(b.result())
    }
  }

  override def toJsAny: js.Any = {
    val length = value.length

    if (length == 0) {
      js.Dictionary[js.Any]().asInstanceOf[js.Object]
    } else {
      val dict = js.Dictionary[js.Any]()
      var index = 0
      while (index < length) {
        val v = value(index)
        dict(v.field) = v.value.toJsAny
        index += 1
      }
      dict.asInstanceOf[js.Object]
    }
  }

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case jObject: JObject =>
        val length = value.length
        if (length != jObject.value.length)
          return false
        var index = 0
        while (index < length) {
          if (value(index) != jObject.value(index))
            return false
          index += 1
        }
        true
      case _ => false
    }
  }

  override def hashCode: Int = {
    var index = 0
    var result = 1

    while (index < value.length) {
      val elem = value(index)
      result = 31 * result + (if (elem == null) 0
                              else {
                                result = 31 * result + elem.field.##
                                elem.value match {
                                  case unsafe.JNull => unsafe.JNull.##
                                  case unsafe.JString(s) => s.##
                                  case unsafe.JBoolean(b) => b.##
                                  case unsafe.JNumber(i, _) => i.##
                                  case unsafe.JArray(a) => a.##
                                  case unsafe.JObject(obj) => obj.##
                                }
                              })
      index += 1
    }
    result
  }
}

object JArray {
  import js.JSConverters._
  def apply(value: JValue, values: JValue*): JArray =
    JArray(js.Array(value) ++ values.toJSArray)

  def apply(value: Array[JValue]): JArray = JArray(value.toJSArray)
}

/** Represents a JSON Array value
  * @author Matthew de Detrich
  */
// JArray is internally represented as a mutable js.Array, to improve sequential performance
final case class JArray(value: js.Array[JValue] = js.Array()) extends JValue {
  override def toStandard: ast.JValue = {
    // Javascript array.length across all major browsers has near constant cost, so we
    // use this to build the array http://jsperf.com/length-comparisons
    val length = value.length
    if (length == 0) {
      ast.JArray(Vector.newBuilder[ast.JValue].result())
    } else {
      val b = Vector.newBuilder[ast.JValue]
      var index = 0
      while (index < length) {
        b += value(index).toStandard
        index += 1
      }
      ast.JArray(b.result())
    }
  }

  override def toJsAny: js.Any = value

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case jArray: JArray =>
        val length = value.length
        if (length != jArray.value.length)
          return false
        var index = 0
        while (index < length) {
          if (value(index) != jArray.value(index))
            return false
          index += 1
        }
        true
      case _ => false
    }
  }

  override def hashCode: Int = {
    var index = 0
    var result = 1

    while (index < value.length) {
      val elem = value(index)
      result = 31 * result + (if (elem == null) 0
                              else {
                                elem match {
                                  case unsafe.JNull => unsafe.JNull.##
                                  case unsafe.JString(s) => s.##
                                  case unsafe.JBoolean(b) => b.##
                                  case unsafe.JNumber(i, _) => i.##
                                  case unsafe.JArray(a) => a.##
                                  case unsafe.JObject(obj) => obj.##
                                }
                              })
      index += 1
    }
    result
  }
}
