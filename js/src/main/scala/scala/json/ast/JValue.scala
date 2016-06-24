package scala.json.ast

import scala.scalajs.js

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
  private val undefined = js.undefined

  private def unsafeAny2JValue(input: Any): JValue = input match {
    case null        => JNull
    case s: String   => JString(s)
    case b: Boolean  => JBoolean(b)
    case d: Double   => JNumber(d.toString)
    case `undefined` => JNull

    case a: js.Array[js.Dynamic @unchecked] =>
      JArray(a.map(v => unsafeAny2JValue(v)).toVector)

    case o: js.Object =>
      JObject(o.asInstanceOf[js.Dictionary[js.Dynamic]]
        .map { case (k, v) => k -> unsafeAny2JValue(v) }.toMap)

    case _ => throw new IllegalArgumentException()
  }

  /**
    * Converts a Javascript object/value coming from Javascript to a [[JValue]].
    *
    * @return
    */
  def fromJsAny(json: js.Any): Option[JValue] =
    try {
      Some(unsafeAny2JValue(json))
    } catch {
      case e: IllegalArgumentException => None
    }
}

/** Represents a JSON null value
  *
  * @author Matthew de Detrich
  */
case object JNull extends JValue {
  override def toUnsafe: unsafe.JValue = unsafe.JNull

  override def toJsAny: js.Any = null
}

/** Represents a JSON string value
  *
  * @author Matthew de Detrich
  */
case class JString(value: String) extends JValue {
  override def toUnsafe: unsafe.JValue = unsafe.JString(value)

  override def toJsAny: js.Any = value
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
case class JNumber(value: String) extends JValue {

  if (!value.matches(jNumberRegex)) {
    throw new NumberFormatException(value)
  }

  def to[B](implicit jNumberConverter: JNumberConverter[B]) =
    jNumberConverter(value)

  /**
    * Javascript specification for numbers specify a `Double`, so this is the default export method to `Javascript`
    *
    * @param value
    */
  def this(value: Double) = this(value.toString)

  override def toUnsafe: unsafe.JValue = unsafe.JNumber(value)

  override def toJsAny: js.Any = value.toDouble match {
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
case object JTrue extends JBoolean {
  override def get = true

  override def toUnsafe: unsafe.JValue = unsafe.JTrue
}

/** Represents a JSON Boolean false value
  *
  * @author Matthew de Detrich
  */
case object JFalse extends JBoolean {
  override def get = false

  override def toUnsafe: unsafe.JValue = unsafe.JFalse
}

/** Represents a JSON Object value. Keys must be unique
  * and are unordered
  *
  * @author Matthew de Detrich
  */
case class JObject(value: Map[String, JValue] = Map.empty) extends JValue {

  /**
    * Construct a JObject using Javascript's object type, i.e. {} or new Object
    *
    * @param value
    */
  def this(value: js.Dictionary[JValue]) = {
    this(value.toMap)
  }

  override def toUnsafe: unsafe.JValue = {
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

  override def toJsAny: js.Any = {
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
case class JArray(value: Vector[JValue] = Vector.empty) extends JValue {

  /**
    *
    * Construct a JArray using Javascript's array type, i.e. `[]` or `new Array`
    *
    * @param value
    */
  def this(value: js.Array[JValue]) = {
    this(value.to[Vector])
  }

  override def toUnsafe: unsafe.JValue = {
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

  override def toJsAny: js.Any = {
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
