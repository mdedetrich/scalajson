package scalajson.ast
package unsafe

import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds
import scalajson.ast
import scalajson.ast._

/** Represents a JSON Value which may be invalid. Internally uses mutable
  * collections when its desirable to do so, for performance and other reasons
  * (such as ordering and duplicate keys)
  *
  * @author Matthew de Detrich
  * @see https://www.ietf.org/rfc/rfc4627.txt
  */
sealed abstract class JValue extends Serializable with Product {

  private[unsafe] type CBF[C[A, B] <: Map[A, B]] = CanBuildFrom[Nothing, (String, ast.JValue), C[String, ast.JValue]]
  /**
    * Converts a [[unsafe.JValue]] to a [[ast.JValue]]. Note that
    * when converting [[unsafe.JNumber]], this can throw runtime error if the underlying
    * string representation is not a correct number. Also when converting a [[unsafe.JObject]]
    * to a [[ast.JObject]], its possible to lose data if you have duplicate keys.
    *
    * @tparam C An immutable Map abstraction, by default its [[scala.collection.immutable.Map]]
    * @see https://www.ietf.org/rfc/rfc4627.txt
    * @return
    */
  def toStandard[C[A, B] <: Map[A, B]](implicit cbf: CBF[C]): ast.JValue
}

/** Represents a JSON null value
  *
  * @author Matthew de Detrich
  */
final case object JNull extends JValue {
  override def toStandard[C[A, B] <: Map[A, B]](implicit cbf: CBF[C]): ast.JValue = ast.JNull
}

/** Represents a JSON string value
  *
  * @author Matthew de Detrich
  */
final case class JString(value: String) extends JValue {
  override def toStandard[C[A, B] <: Map[A, B]](implicit cbf: CBF[C]): ast.JValue = ast.JString(value)
}

object JNumber {
  def apply(value: Int): JNumber = JNumber(value.toInt.toString)

  def apply(value: Long): JNumber = JNumber(value.toString)

  def apply(value: BigInt): JNumber = JNumber(value.toString)

  def apply(value: BigDecimal): JNumber = JNumber(value.toString)

  def apply(value: Float): JNumber = JNumber(value.toString)

  def apply(value: Double): JNumber = JNumber(value.toString)

  def apply(value: Integer): JNumber = JNumber(value.toString)

  def apply(value: Array[Char]): JNumber = JNumber(new String(value))
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
final case class JNumber(value: String) extends JValue {
  override def toStandard[C[A, B] <: Map[A, B]](implicit cbf: CBF[C]): ast.JValue =
    value match {
      case jNumberRegex(_ *) => new ast.JNumber(value)
      case _ => throw new NumberFormatException(value)
    }

  def toInt: Option[Int] = scalajson.ast.toInt(value)

  def toBigInt: Option[BigInt] = scalajson.ast.toBigInt(value)

  def toLong: Option[Long] = scalajson.ast.toLong(value)

  def toDouble: Double = scalajson.ast.toDouble(value)

  def toFloat: Float = scalajson.ast.toFloat(value)

  def toBigDecimal: Option[BigDecimal] = scalajson.ast.toBigDecimal(value)
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
final case object JTrue extends JBoolean {
  override def get = true

  override def toStandard[C[A, B] <: Map[A, B]](implicit cbf: CBF[C]): ast.JValue = ast.JTrue
}

/** Represents a JSON Boolean false value
  *
  * @author Matthew de Detrich
  */
final case object JFalse extends JBoolean {
  override def get = false

  override def toStandard[C[A, B] <: Map[A, B]](implicit cbf: CBF[C]): ast.JValue = ast.JFalse
}

final case class JField(field: String, value: JValue)

object JObject {
  def apply(value: JField, values: JField*): JObject =
    JObject(Array(value) ++ values)
}

/** Represents a JSON Object value. Duplicate keys
  * are allowed and ordering is respected
  * @author Matthew de Detrich
  */
// JObject is internally represented as a mutable Array, to improve sequential performance
final case class JObject(value: Array[JField] = Array.empty) extends JValue {
  override def toStandard[C[A, B] <: Map[A, B]](implicit cbf: CBF[C]): ast.JValue = {
    val length = value.length
    val b = cbf()
    if (length == 0) {
      ast.JObject(b.result())
    } else {
      var index = 0
      while (index < length) {
        val v = value(index)
        b += ((v.field, v.value.toStandard))
        index += 1
      }
      ast.JObject(b.result())
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

  override def hashCode: Int =
    java.util.Arrays.deepHashCode(value.asInstanceOf[Array[AnyRef]])

  override def toString =
    "JObject(" + java.util.Arrays.toString(value.asInstanceOf[Array[AnyRef]]) + ")"
}

object JArray {
  def apply(value: JValue, values: JValue*): JArray =
    JArray(Array(value) ++ values.toArray[JValue])
}

/** Represents a JSON Array value
  * @author Matthew de Detrich
  */
// JArray is internally represented as a mutable Array, to improve sequential performance
final case class JArray(value: Array[JValue] = Array.empty) extends JValue {
  override def toStandard[C[A, B] <: Map[A, B]](implicit cbf: CBF[C]): ast.JValue = {
    val length = value.length
    if (length == 0) {
      ast.JArray(Vector[ast.JValue]())
    } else {
      var index = 0
      val b = Vector.newBuilder[ast.JValue]
      while (index < length) {
        b += value(index).toStandard
        index += 1
      }
      ast.JArray(b.result())
    }
  }

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

  override def hashCode: Int =
    java.util.Arrays.deepHashCode(value.asInstanceOf[Array[AnyRef]])

  override def toString =
    "JArray(" + java.util.Arrays.toString(value.asInstanceOf[Array[AnyRef]]) + ")"
}
