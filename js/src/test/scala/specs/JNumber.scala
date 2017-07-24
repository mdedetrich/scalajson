package specs

import org.scalacheck.Prop.forAll
import utest._

object JNumber extends TestSuite with UTestScalaCheck {

  val tests = TestSuite {
    "The JNumber value should" - {
      "read a Long" - readLongJNumber
      "read a BigDecimal" - readBigDecimalJNumber
      "read a Double" - readDoubleJNumber
      "read a BigInt" - readBigIntJNumber
      "read an Int" - readIntJNumber
      "read a Double NaN" - readDoubleNANJNumber
      "read a Double Positive Infinity" - readDoublePositiveInfinityJNumber
      "read a Double Negative Infinity" - readDoubleNegativeInfinityJNumber
      "read a Float" - readFloatJNumber
      "read a Float NaN" - readFloatNANJNumber
      "read a Float Positive Infinity" - readFloatPositiveInfinityJNumber
      "read a Float Negative Infinity" - readFloatNegativeInfinityJNumber
      "read a Short" - readShortJNumber
      "hashCode equals decimal" - hashCodeEqualsDecimal
      "hashCode equals decimal #2" - hashCodeEqualsDecimal2
      "hashCode not equals decimal" - hashCodeNotEqualsDecimal
      "hashCode not equals decimal #2" - hashCodeNotEqualsDecimal2
      "hashCode equals e" - hashCodeEqualsE
      "hashCode equals e #2" - hashCodeEqualsE2
      "convert to jsAny" - toJsAny
      "hashCode equals e negative" - hashCodeEqualsENegative
      "hashCode equals e negative" - hashCodeEqualsENegative2
      "hashCode not equals e negative" - hashCodeNotEqualsENegative
      "hashCode not equals e negative #2" - hashCodeNotEqualsENegative2
      "hashCode equals e positive" - hashCodeEqualsEPositive
      "hashCode equals e positive #2" - hashCodeEqualsEPositive2
      "hashCode not equals e positive" - hashCodeNotEqualsEPositive
      "hashCode not equals e positive #2" - hashCodeNotEqualsEPositive2
      "convert toUnsafe" - toUnsafe
      "equals" - testEquals
    }

    def readBigDecimalJNumber =
      forAll { b: BigDecimal =>
        scalajson.ast.JNumber(b).toBigDecimal == Option(b)
      }.checkUTest()

    def readBigIntJNumber =
      forAll { b: BigInt =>
        scalajson.ast.JNumber(b).toBigInt == Option(b)
      }.checkUTest()

    def readLongJNumber =
      forAll { l: Long =>
        scalajson.ast.JNumber(l).toLong == Option(l)
      }.checkUTest()

    def readIntJNumber =
      forAll { i: Int =>
        scalajson.ast.JNumber(i).toInt == Option(i)
      }.checkUTest()

    def readDoubleJNumber =
      forAll { d: Double =>
        scalajson.ast.JNumber(d) match {
          case scalajson.ast.JNull => JNull == JNull
          case scalajson.ast.JNumber(value) => value == d.toString
        }
      }.checkUTest()

    def readDoubleNANJNumber = {
      scalajson.ast.JNumber(Double.NaN) match {
        case scalajson.ast.JNull => true
        case _ => false
      }
    }

    def readDoublePositiveInfinityJNumber = {
      scalajson.ast.JNumber(Double.PositiveInfinity) match {
        case scalajson.ast.JNull => true
        case _ => false
      }
    }

    def readDoubleNegativeInfinityJNumber = {
      scalajson.ast.JNumber(Double.NegativeInfinity) match {
        case scalajson.ast.JNull => true
        case _ => false
      }
    }

    def readFloatJNumber =
      forAll { f: Float =>
        scalajson.ast.JNumber(f) match {
          case scalajson.ast.JNull => JNull == JNull
          case scalajson.ast.JNumber(value) => value == f.toString
        }
      }.checkUTest()

    def readFloatNANJNumber = {
      scalajson.ast.JNumber(Float.NaN) match {
        case scalajson.ast.JNull => true
        case _ => false
      }
    }

    def readFloatPositiveInfinityJNumber = {
      scalajson.ast.JNumber(Float.PositiveInfinity) match {
        case scalajson.ast.JNull => true
        case _ => false
      }
    }

    def readFloatNegativeInfinityJNumber = {
      scalajson.ast.JNumber(Float.NegativeInfinity) match {
        case scalajson.ast.JNull => true
        case _ => false
      }
    }

    def readShortJNumber =
      forAll { s: Short =>
        scalajson.ast.JNumber(s).stringValue == s.toString
      }.checkUTest()

    def hashCodeEqualsDecimal = {
      scalajson.ast.JNumber.fromString("34").get.## == scalajson.ast.JNumber
        .fromString("34.0")
        .get
        .##
    }

    def hashCodeEqualsDecimal2 = {
      scalajson.ast.JNumber.fromString("34").get.## == scalajson.ast.JNumber
        .fromString("34.00")
        .get
        .##
    }

    def hashCodeNotEqualsDecimal = {
      scalajson.ast.JNumber.fromString("34").get.## == scalajson.ast.JNumber
        .fromString("34.01")
        .get
        .##
    }

    def hashCodeNotEqualsDecimal2 = {
      scalajson.ast.JNumber.fromString("34").get.## == scalajson.ast.JNumber
        .fromString("34.001")
        .get
        .##
    }

    def hashCodeEqualsE = {
      scalajson.ast.JNumber.fromString("34e34").get.## != scalajson.ast.JNumber
        .fromString("34e034")
        .get
        .##
    }

    def hashCodeEqualsE2 = {
      scalajson.ast.JNumber.fromString("34e34").get.## != scalajson.ast.JNumber
        .fromString("34e0034")
        .get
        .##
    }

    def hashCodeEqualsENegative = {
      JNumber("34e-0").## == JNumber("34").##
    }

    def hashCodeEqualsENegative2 = {
      JNumber("34e-00").## == JNumber("34").##
    }

    def hashCodeNotEqualsENegative = {
      JNumber("34e-01").## != JNumber("34").##
    }

    def hashCodeNotEqualsENegative2 = {
      JNumber("34e-001").## != JNumber("34").##
    }

    def hashCodeEqualsEPositive = {
      JNumber("34e+0").## == JNumber("34").##
    }

    def hashCodeEqualsEPositive2 = {
      JNumber("34e+00").## == JNumber("34").##
    }

    def hashCodeNotEqualsEPositive = {
      JNumber("34e+01").## != JNumber("34").##
    }

    def hashCodeNotEqualsEPositive2 = {
      JNumber("34e+001").## != JNumber("34").##
    }

    def toJsAny =
      forAll { d: Double =>
        scalajson.ast.JNumber(d).toJsAny == d
      }.checkUTest()

    def toUnsafe =
      forAll { b: BigDecimal =>
        scalajson.ast.JNumber(b).toUnsafe == scalajson.ast.unsafe.JNumber(b)
      }.checkUTest()

    def testEquals =
      forAll { b: BigDecimal =>
        scalajson.ast.JNumber(b) == scalajson.ast.JNumber(b)
      }.checkUTest()
  }
}
