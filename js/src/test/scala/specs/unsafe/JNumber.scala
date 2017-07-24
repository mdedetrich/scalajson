package specs.unsafe

import specs.UTestScalaCheck
import utest._
import org.scalacheck.Prop._
import scalajson.ast.unsafe.JNull

object JNumber extends TestSuite with UTestScalaCheck {

  val tests = TestSuite {
    "The unsafe.JNumber value should" - {
      "read a Long" - readLongJNumber
      "read a BigDecimal" - readBigDecimalJNumber
      "read a BigInt" - readBigIntJNumber
      "read an Int" - readIntJNumber
      "read a Double" - readDoubleJNumber
      "read a Double NaN" - readDoubleNANJNumber
      "read a Double Positive Infinity" - readDoublePositiveInfinityJNumber
      "read a Double Negative Infinity" - readDoubleNegativeInfinityJNumber
      "read a Float" - readFloatJNumber
      "read a Short" - readShortJNumber
      "read a String and not fail" - readStringJNumber
      "read a String and detect non numeric numbers" - readStringJNumberDetect
      "convert to jsAny" - toJsAny
      "convert toStandard" - toStandard
    }
  }

  def readBigDecimalJNumber =
    forAll { b: BigDecimal =>
      scalajson.ast.unsafe.JNumber(b).toBigDecimal == Option(b)
    }.checkUTest()

  def readBigIntJNumber =
    forAll { b: BigInt =>
      scalajson.ast.unsafe.JNumber(b).toBigInt == Option(b)
    }.checkUTest()

  def readLongJNumber =
    forAll { l: Long =>
      scalajson.ast.unsafe.JNumber(l).toLong == Option(l)
    }.checkUTest()

  def readIntJNumber =
    forAll { i: Int =>
      scalajson.ast.unsafe.JNumber(i).toInt == Option(i)
    }.checkUTest()

  def readDoubleJNumber =
    forAll { d: Double =>
      scalajson.ast.unsafe.JNumber(d) match {
        case JNull                               => JNull == JNull
        case scalajson.ast.unsafe.JNumber(value) => value == d.toString
      }
    }.checkUTest()

  def readDoubleNANJNumber = {
    scalajson.ast.unsafe.JNumber(Double.NaN) match {
      case JNull => true
      case _ => false
    }
  }

  def readDoublePositiveInfinityJNumber = {
    scalajson.ast.unsafe.JNumber(Double.PositiveInfinity) match {
      case JNull => true
      case _ => false
    }
  }

  def readDoubleNegativeInfinityJNumber = {
    scalajson.ast.unsafe.JNumber(Double.NegativeInfinity) match {
      case JNull => true
      case _ => false
    }
  }

  def readFloatJNumber =
    forAll { f: Float =>
      scalajson.ast.unsafe.JNumber(f) match {
        case JNull                               => JNull == JNull
        case scalajson.ast.unsafe.JNumber(value) => value == f.toString
      }
    }.checkUTest()

  def readShortJNumber =
    forAll { s: Short =>
      scalajson.ast.unsafe.JNumber(s).stringValue == s.toString
    }.checkUTest()

  def readStringJNumber =
    forAll { s: String =>
      scalajson.ast.unsafe.JNumber(s).get.stringValue == s.toString
    }.checkUTest()

  def readStringJNumberDetect =
    forAll { s: String =>
      {
        scala.util
          .Try {
            BigDecimal(s)
          }
          .toOption
          .isEmpty
      } ==> {
        scala.util
          .Try(BigDecimal(scalajson.ast.unsafe.JNumber(s).get.stringValue))
          .toOption
          .isEmpty == true
      }
    }.checkUTest()

  def toJsAny =
    forAll { d: Double =>
      scalajson.ast.unsafe.JNumber(d).toJsAny == d
    }.checkUTest()

  def toStandard =
    forAll { b: BigDecimal =>
      scalajson.ast.unsafe.JNumber(b).toStandard == scalajson.ast.JNumber(b)
    }.checkUTest()
}
