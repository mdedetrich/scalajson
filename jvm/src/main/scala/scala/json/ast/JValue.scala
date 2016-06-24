package scala.json.ast

/** Represents a valid JSON Value
  *
  * @author Matthew de Detrich
  * @see https://www.ietf.org/rfc/rfc4627.txt
  */
sealed abstract class JValue extends Product with Serializable {

  /**
    * Converts a [[JValue]] to a [[unsafe.JValue]]. Note that
    * when converting [[JObject]], this can produce [[unsafe.JObject]] of
    * unknown ordering, since ordering on a [[scala.collection.Map]] isn't defined. Duplicate keys will also
    * be removed in an undefined manner.
    *
    * @see https://www.ietf.org/rfc/rfc4627.txt
    * @return
    */
  def toUnsafe: unsafe.JValue
}

/** Represents a JSON null value
  *
  * @author Matthew de Detrich
  */
case object JNull extends JValue {
  override def toUnsafe: unsafe.JValue = unsafe.JNull
}

/** Represents a JSON string value
  *
  * @author Matthew de Detrich
  */
case class JString(value: String) extends JValue {
  override def toUnsafe: unsafe.JValue = unsafe.JString(value)
}

/**
  * If you are passing in a NaN or Infinity as a Double, JNumber will
  * return a JNull
  */
object JNumber {
  def apply(value: Int): JNumber = JNumber(value.toString)

  def apply(value: Short): JNumber = JNumber(value.toString)

  def apply(value: Long): JNumber = JNumber(value.toString)

  def apply(value: BigInt): JNumber = JNumber(value.toString)

  def apply(value: Float): JNumber = JNumber(value.toString)

  def apply(value: BigDecimal): JNumber = JNumber(value.toString())

  /**
    * @param value
    * @return Will return a [[JNull]] if value is a Nan or Infinity
    */
  def apply(value: Double): JValue = value match {
    case n if n.isNaN => JNull
    case n if n.isInfinity => JNull
    case _ => JNumber(value.toString)
  }

  def apply(value: Integer): JNumber = JNumber(value.toString)

  def apply(value: Array[Char]): JNumber = JNumber(value.toString)
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

  override def toUnsafe: unsafe.JValue = unsafe.JNumber(value)

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
  override def toUnsafe: unsafe.JValue = {
    if (value.isEmpty) {
      unsafe.JArray(Array.ofDim[unsafe.JValue](0))
    } else {
      val array = Array.ofDim[unsafe.JField](value.size)
      var index = 0
      for ((k, v) <- value) {
        array(index) = unsafe.JField(k, v.toUnsafe)
        index += 1
      }
      unsafe.JObject(array)
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
  override def toUnsafe: unsafe.JValue = {
    val length = value.length
    if (length == 0) {
      unsafe.JArray(Array.ofDim[unsafe.JValue](0))
    } else {
      val array = Array.ofDim[unsafe.JValue](length)
      val iterator = value.iterator
      var index = 0
      while (iterator.hasNext) {
        array(index) = iterator.next().toUnsafe
        index += 1
      }
      unsafe.JArray(array)
    }
  }
}
