package scalajson.ast
package unsafe

import scalajson.ast
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

/**
 * Represents a JSON number.
 */
sealed abstract class JNumber extends JValue {
  override def equals(obj: Any): Boolean = JNumber.jnumberEquals(this, obj)

  override def toString: String = stringValue

  /** String representation of the number. */
  def stringValue: String

  override def toStandard: ast.JValue = JNumber.toStandard(this)

  override def hashCode: Int = JNumber.jnumberHashCode(this)

  /**
   * Return this number as a [[scala.math.BigDecimal]].
   */
  def toBigDecimal: Option[BigDecimal]

  /**
   * Return this number as a [[scala.math.BigInt]] if it's a sufficiently small whole number.
   */
  def toBigInt: Option[BigInt]

  /**
   * Return this number as a [[scala.Long]] if it's a valid [[scala.Long]].
   */
  def toLong: Option[Long]

  /**
   * Return this number as a [[scala.Int]] if it's a valid [[scala.Int]].
   */
  def toInt: Option[Int]

  /**
   * Convert this number to its best [[scala.Double]] approximation.
   */
  def toDouble: Double

  override def toJsAny: js.Any = toDouble match {
    case n if n.isNaN => null
    case n if n.isInfinity => null
    case n => n
  }
}

/**
 * Constructors, type class instances, and other utilities for [[JNumber]].
 */
object JNumber {
  def apply(value: BigDecimal): JNumber = JBigDecimal(value)
  def apply(value: BigInt): JNumber = JBigInt(value)
  def apply(value: Long): JNumber = JLong(value)
  def apply(value: Int): JNumber = JInt(value)
  def apply(value: Short): JNumber = JInt(value.toInt)

  /**
    * @param value
    * @return Will return a [[JNull]] if value is a Nan or Infinity
    */
  def apply(value: Double): JValue = fromDouble(value)

  /**
    * @param value
    * @return Will return a [[JNull]] if value is a Nan or Infinity
    */
  def fromDouble(value: Double): JValue = value match {
    case n if n.isNaN => JNull
    case n if n.isInfinity => JNull
    case _ => JDouble(value)
  }

  /**
    * @param value
    * @return Will return a [[JNull]] if value is a Nan or Infinity
    */
  def apply(value: Float): JValue = fromFloat(value)

  /**
    * @param value
    * @return Will return a [[JNull]] if value is a Nan or Infinity
    */
  def fromFloat(value: Float): JValue = value match {
    case n if java.lang.Float.isNaN(n) => JNull
    case n if n.isInfinity => JNull
    case _ => JFloat(value)
  }

  def apply(value: Array[Char]): Option[JNumber] = fromString(new String(value))

  def apply(value: String): Option[JNumber] = fromString(value)

  def fromString(value: String): Option[JNumber] = Option(JStringDecimal(value))

  def unapply(value: JNumber): Option[String] = Option(value.stringValue)

  private[ast] def jnumberEquals(value: JNumber, obj: Any): Boolean =
    (value, obj) match {
      case (JStringDecimal(x), JStringDecimal(y)) => x == y  
      case (JBigDecimal(x), JBigDecimal(y))       => x == y
      case (JBigInt(x), JBigInt(y))               => x == y
      case (JLong(x), JLong(y))                   => x == y
      case (JInt(x), JInt(y))                     => x == y
      case (JDouble(x), JDouble(y))               => x == y
      case (JFloat(x), JFloat(y))                 => x == y
      case (x, y: JNumber)  => x.stringValue == y.stringValue
      case _                => false
    }

  private[ast] def jnumberHashCode(value: JNumber): Int =
    numericStringHashcode(value.stringValue)

  private[ast] def toStandard(value: JNumber): ast.JValue =
    value match {
      case JStringDecimal(value) => ast.JStringDecimal(value)
      case JBigDecimal(value)    => ast.JBigDecimal(value)
      case JBigInt(value)        => ast.JBigInt(value)
      case JLong(value)          => ast.JLong(value)
      case JInt(value)           => ast.JInt(value)
      case JDouble(value)        => ast.JDouble(value)
      case JFloat(value)         => ast.JFloat(value)
    }
}

/**
 * Represent a JSON number as a `String`.
 */
private[ast] final case class JStringDecimal(value: String) extends JNumber {
  override def stringValue: String = value

  // "1e2147483648" is a valid JSON number, but will overflow BigDecimal.
  override final def toBigDecimal: Option[BigDecimal] =
    try {
      Option(BigDecimal(value))
    } catch {
      case _: NumberFormatException => None
    }

  override final def toBigInt: Option[BigInt] =
    try {
      Option(BigInt(value))
    } catch {
      case _: NumberFormatException => None
    }

  override final def toLong: Option[Long] =
    try {
      Option(value.toLong)
    } catch {
      case _: NumberFormatException => None
    }

  override final def toInt: Option[Int] =
    try {
      Option(value.toInt)
    } catch {
      case _: NumberFormatException => None
    }

  override final def toDouble: Double = value.toDouble
}

/**
 * Represent a valid JSON number as a [[scala.math.BigDecimal]].
 */
private[ast] final case class JBigDecimal(value: BigDecimal) extends JNumber {
  override def stringValue: String = value.toString
  override final def toBigDecimal: Option[BigDecimal] = Option(value)
  override final def toBigInt: Option[BigInt] = value.toBigIntExact

  override final def toLong: Option[Long] =
    if (value.isValidLong) Option(value.toLong)
    else None

  override final def toInt: Option[Int] =
    if (value.isValidInt) Option(value.toInt)
    else None

  final def toDouble: Double = value.doubleValue
}

/**
 * Represent a valid JSON number as a [[scala.BigInt]].
 */
private[ast] final case class JBigInt(value: BigInt) extends JNumber {
  override def stringValue: String = value.toString
  override final def toBigDecimal: Option[BigDecimal] = Option(BigDecimal(value))
  override final def toBigInt: Option[BigInt] = Option(value)

  override final def toLong: Option[Long] =
    if (value.isValidLong) Option(value.toLong)
    else None

  override final def toInt: Option[Int] =
    if (value.isValidLong) Option(value.toInt)
    else None

  final def toDouble: Double = value.doubleValue
}

/**
 * Represent a valid JSON number as a [[scala.Long]].
 */
private[ast] final case class JLong(value: Long) extends JNumber {
  override def stringValue: String = java.lang.Long.toString(value)
  override final def toBigDecimal: Option[BigDecimal] = Option(BigDecimal(value))
  override final def toBigInt: Option[BigInt] = Option(BigInt(value))
  override final def toLong: Option[Long] = Option(value)

  override final def toInt: Option[Int] = {
    val asBigDecimal = BigDecimal(value)
    if (asBigDecimal.isValidInt) Option(asBigDecimal.toInt)
    else None 
  }

  final def toDouble: Double = value.toDouble
}

/**
 * Represent a valid JSON number as a [[scala.Int]].
 */
private[ast] final case class JInt(value: Int) extends JNumber {
  override def stringValue: String = java.lang.Integer.toString(value)
  override final def toBigDecimal: Option[BigDecimal] = Option(BigDecimal(value))
  override final def toBigInt: Option[BigInt] = Option(BigInt(value))
  override final def toLong: Option[Long] = Option(value.toLong)
  override final def toInt: Option[Int] = Option(value)
  override final def toDouble: Double = value.toDouble
}

/**
 * Represent a valid JSON number as a [[scala.Double]].
 */
private[ast] final case class JDouble(value: Double) extends JNumber {
  override def stringValue: String = java.lang.Double.toString(value)
  override final def toBigDecimal: Option[BigDecimal] = Option(BigDecimal(value))
  override final def toBigInt: Option[BigInt] = JDouble.toBigInt(value)
  override final def toLong: Option[Long] = JDouble.toLong(value)
  override final def toInt: Option[Int] = JDouble.toInt(value)
  override final def toDouble: Double = value
}

private [ast] object JDouble {
  def toBigInt(value: Double): Option[BigInt] = {
    val asBigDecimal = BigDecimal(value)
    if (asBigDecimal.isWhole) asBigDecimal.toBigIntExact
    else None 
  }

  def toLong(value: Double): Option[Long] = {
    val asBigDecimal = BigDecimal(value)
    if (asBigDecimal.isValidLong) Option(asBigDecimal.toLong)
    else None
  }

  def toInt(value: Double): Option[Int] = {
    val asBigDecimal = BigDecimal(value)
    if (asBigDecimal.isValidInt) Option(asBigDecimal.toInt)
    else None
  }
}

/**
 * Represent a valid JSON number as a [[scala.Float]].
 */
private[ast] final case class JFloat(value: Float) extends JNumber {
  override def stringValue: String = java.lang.Float.toString(value)
  override final def toBigDecimal: Option[BigDecimal] = Option(BigDecimal(value.toDouble))
  override final def toBigInt: Option[BigInt] = JDouble.toBigInt(value.toDouble)
  override final def toLong: Option[Long] = JDouble.toLong(value.toDouble)
  override final def toInt: Option[Int] = JDouble.toInt(value.toDouble)
  override final def toDouble: Double = value.toDouble
}

/** Represents a JSON Boolean value, which can either be a
  * [[JTrue]] or a [[JFalse]]
  *
  * @author Matthew de Detrich
  */
// Implements named extractors so we can avoid boxing
sealed abstract class JBoolean extends JValue {
  def isEmpty: Boolean = false
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
                                  case unsafe.JNumber(i) => i.##
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
                                  case unsafe.JNumber(i) => i.##
                                  case unsafe.JArray(a) => a.##
                                  case unsafe.JObject(obj) => obj.##
                                }
                              })
      index += 1
    }
    result
  }
}
