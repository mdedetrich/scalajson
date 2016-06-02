package scala.json.ast

import scala.annotation.meta.field
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

/** Represents a valid JSON Value
  *
  * @author Matthew de Detrich
  * @see https://www.ietf.org/rfc/rfc4627.txt
  */
sealed abstract class JValue extends Product with Serializable {

  /**
    * Converts a [[JValue]] to a [[unsafe.JValue]]. Note that
    * when converting [[JObject]], this can produce [[unsafe.JObject]] of
    * unknown ordering, since ordering on a [[scala.collection.Map]] isn't defined.
    * Duplicate keys will also be removed in an undefined manner.
    *
    * @see https://www.ietf.org/rfc/rfc4627.txt
    * @return
    */
  def toUnsafe: unsafe.JValue

  /**
    * Converts a [[JValue]] to a Javascript object/value that can be used within
    * Javascript
    *
    * @return
    */
  def toJsAny: js.Any
}

object JValue {
  /**
    * Converts a Javascript object/value coming from Javascript to a [[JValue]].
    *
    * @return
    */
  def fromJsAny(json: js.Any): JValue =
    json match {
      case v if v == null => JNull
      case v if v.isInstanceOf[Boolean] =>
        if (v.asInstanceOf[Boolean]) JTrue else JFalse

      case v if (v: Any).isInstanceOf[String] =>
        JString(v.asInstanceOf[String])

      case v: js.Array[js.Any @unchecked] =>
        JArray(v.map(fromJsAny).toVector)

      case v if js.typeOf(v) == "object" =>
        JObject(v.asInstanceOf[js.Dictionary[js.Any]].mapValues(fromJsAny).toMap)

      case v if js.typeOf(v) == "number" =>
        JNumber(v.toString)
    }
}

/** Represents a JSON null value
  *
  * @author Matthew de Detrich
  */
@JSExport
case object JNull extends JValue {
  @JSExport override def toUnsafe: unsafe.JValue = unsafe.JNull

  @JSExport override def toJsAny: js.Any = null
}

/** Represents a JSON string value
  *
  * @author Matthew de Detrich
  */
@JSExport
case class JString(@(JSExport @field) value: String) extends JValue {
  @JSExport override def toUnsafe: unsafe.JValue = unsafe.JString(value)

  @JSExport override def toJsAny: js.Any = value
}

object JNumber {
  def apply(value: Int): JNumber = JNumber(value.toString)

  def apply(value: Integer): JNumber = JNumber(value.toString)

  def apply(value: Short): JNumber = JNumber(value.toString)

  def apply(value: Long): JNumber = JNumber(value.toString)

  def apply(value: BigInt): JNumber = JNumber(value.toString())

  def apply(value: BigDecimal): JNumber = JNumber(value.toString())

  /**
    * @param value
    * @return Will return a JNull if value is a Nan or Infinity
    */
  def apply(value: Double): JValue = value match {
    case n if n.isNaN => JNull
    case n if n.isInfinity => JNull
    case _ => JNumber(value.toString)
  }

  def apply(value: Float): JNumber =
    JNumber(value.toString) // In Scala.js, float has the same representation as double
}

/** Represents a JSON number value. If you are passing in a
  * NaN or Infinity as a [[Double]], [[JNumber]] will
  * return a [[JNull]].
  *
  * @author Matthew de Detrich
  * @throws NumberFormatException - If the value is not a valid JSON Number
  */
@JSExport
case class JNumber(@(JSExport @field) value: String) extends JValue {

  if (!value.matches(jNumberRegex)) {
    throw new NumberFormatException(value)
  }

  def to[B](implicit bigDecimalConverter: JNumberConverter[B]) =
    bigDecimalConverter(value)

  /**
    * Javascript specification for numbers specify a `Double`, so this is the default export method to `Javascript`
    *
    * @param value
    */
  @JSExport def this(value: Double) = this(value.toString)

  @JSExport override def toUnsafe: unsafe.JValue = unsafe.JNumber(value)

  @JSExport override def toJsAny: js.Any = value.toDouble match {
    case n if n.isNaN => null
    case n if n.isInfinity => null
    case n => n
  }

  override def equals(a: Any) =
    a match {
      case jNumber: JNumber => numericStringEquals(value, jNumber.value)
      case _ => false
    }

  override def hashCode =
    numericStringHashcode(value)
}

/** Represents a JSON Boolean value, which can either be a
  * [[JTrue]] or a [[JFalse]]
  *
  * @author Matthew de Detrich
  */
// Implements named extractors so we can avoid boxing
sealed abstract class JBoolean extends JValue {
  def get: Boolean

  @JSExport override def toJsAny: js.Any = get
}

object JBoolean {
  def apply(x: Boolean): JBoolean = if (x) JTrue else JFalse

  def unapply(x: JBoolean): Some[Boolean] = Some(x.get)
}

/** Represents a JSON Boolean true value
  *
  * @author Matthew de Detrich
  */
@JSExport
case object JTrue extends JBoolean {
  override def get = true

  @JSExport override def toUnsafe: unsafe.JValue = unsafe.JTrue
}

/** Represents a JSON Boolean false value
  *
  * @author Matthew de Detrich
  */
@JSExport
case object JFalse extends JBoolean {
  override def get = false

  @JSExport override def toUnsafe: unsafe.JValue = unsafe.JFalse
}

/** Represents a JSON Object value. Keys must be unique
  * and are unordered
  *
  * @author Matthew de Detrich
  */
case class JObject(@(JSExport @field) value: Map[String, JValue] = Map.empty)
    extends JValue {

  /**
    * Construct a JObject using Javascript's object type, i.e. {} or new Object
    *
    * @param value
    */
  @JSExport def this(value: js.Dictionary[JValue]) = {
    this(value.toMap)
  }

  @JSExport override def toUnsafe: unsafe.JValue = {
    if (value.isEmpty) {
      unsafe.JObject(js.Array[unsafe.JField]())
    } else {
      val iterator = value.iterator
      val array = js.Array[unsafe.JField]()
      while (iterator.hasNext) {
        val (k, v) = iterator.next()
        array.push(unsafe.JField(k, v.toUnsafe))
      }
      unsafe.JObject(array)
    }
  }

  @JSExport override def toJsAny: js.Any = {
    if (value.isEmpty) {
      js.Dictionary[js.Any]().asInstanceOf[js.Object]
    } else {
      val iterator = value.iterator
      val dict = js.Dictionary[js.Any]()
      while (iterator.hasNext) {
        val (k, v) = iterator.next()
        dict(k) = v.toJsAny
      }
      dict.asInstanceOf[js.Object]
    }
  }
}

object JArray {
  def apply(value: JValue, values: JValue*): JArray =
    JArray(value +: values.to[Vector])
}

/** Represents a JSON Array value
  *
  * @author Matthew de Detrich
  */
case class JArray(@(JSExport @field) value: Vector[JValue] = Vector.empty)
    extends JValue {

  /**
    *
    * Construct a JArray using Javascript's array type, i.e. `[]` or `new Array`
    *
    * @param value
    */
  @JSExport def this(value: js.Array[JValue]) = {
    this(value.to[Vector])
  }

  @JSExport override def toUnsafe: unsafe.JValue = {
    if (value.isEmpty) {
      unsafe.JArray(js.Array[unsafe.JValue]())
    } else {
      val iterator = value.iterator
      val array = js.Array[unsafe.JValue]()
      while (iterator.hasNext) {
        array.push(iterator.next().toUnsafe)
      }
      unsafe.JArray(array)
    }
  }

  @JSExport override def toJsAny: js.Any = {
    if (value.isEmpty) {
      js.Array[js.Any]()
    } else {
      val iterator = value.iterator
      val array = js.Array[js.Any]()
      while (iterator.hasNext) {
        array.push(iterator.next().toJsAny)
      }
      array
    }
  }
}
