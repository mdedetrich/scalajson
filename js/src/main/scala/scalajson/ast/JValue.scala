package scalajson.ast

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

/** Represents a JSON null value
  *
  * @author Matthew de Detrich
  */
final case object JNull extends JValue {
  override def toUnsafe: unsafe.JValue = unsafe.JNull

  override def toJsAny: js.Any = null
}

/** Represents a JSON string value
  *
  * @author Matthew de Detrich
  */
final case class JString(value: String) extends JValue {
  override def toUnsafe: unsafe.JValue = unsafe.JString(value)

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

  override def toUnsafe: unsafe.JValue = JNumber.toUnsafe(this)

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

  def fromString(value: String): Option[JNumber] = value match {
    case jNumberRegex(_ *) => Option(JStringDecimal(value))
    case _ => None
  }

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

  private[ast] def toUnsafe(value: JNumber): unsafe.JValue =
    value match {
      case JStringDecimal(value) => unsafe.JStringDecimal(value)
      case JBigDecimal(value)    => unsafe.JBigDecimal(value)
      case JBigInt(value)        => unsafe.JBigInt(value)
      case JLong(value)          => unsafe.JLong(value)
      case JInt(value)           => unsafe.JInt(value)
      case JDouble(value)        => unsafe.JDouble(value)
      case JFloat(value)         => unsafe.JFloat(value)
    }
}

/**
 * Represent a valid JSON number as a `String`.
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

  override def toUnsafe: unsafe.JValue = unsafe.JTrue
}

/** Represents a JSON Boolean false value
  *
  * @author Matthew de Detrich
  */
final case object JFalse extends JBoolean {
  override def get = false

  override def toUnsafe: unsafe.JValue = unsafe.JFalse
}

/** Represents a JSON Object value. Keys must be unique
  * and are unordered
  *
  * @author Matthew de Detrich
  */
final case class JObject(value: Map[String, JValue] = Map.empty)
    extends JValue {

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
      val array = js.Array[unsafe.JField]()
      value.iterator.foreach { x =>
        array.push(unsafe.JField(x._1, x._2.toUnsafe))
      }
      unsafe.JObject(array)
    }
  }

  override def toJsAny: js.Any = {
    if (value.isEmpty) {
      js.Dictionary[js.Any]().asInstanceOf[js.Object]
    } else {
      val dict = js.Dictionary[js.Any]()
      value.iterator.foreach { x =>
        dict(x._1) = x._2.toJsAny
      }
      dict.asInstanceOf[js.Object]
    }
  }
}

object JArray {
  def apply(value: JValue, values: JValue*): JArray =
    JArray(value +: values.toVector)
}

/** Represents a JSON Array value
  *
  * @author Matthew de Detrich
  */
final case class JArray(value: Vector[JValue] = Vector.empty) extends JValue {

  /**
    *
    * Construct a JArray using Javascript's array type, i.e. `[]` or `new Array`
    *
    * @param value
    */
  def this(value: js.Array[JValue]) = {
    this(value.toVector)
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
