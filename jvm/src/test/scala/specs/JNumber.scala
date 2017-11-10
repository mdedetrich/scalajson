package specs

import scalajson.ast._

class JNumber extends Spec {
  def is =
    s2"""
  The JNumber value should
    read a Long $readLongJNumber
    read a BigDecimal $readBigDecimalJNumber
    read a BigInt $readBigIntJNumber
    read an Int $readIntJNumber
    read a Double $readDoubleJNumber
    read a Double NaN $readDoubleNANJNumber
    read a Double Positive Infinity $readDoublePositiveInfinityJNumber
    read a Double Negative Infinity $readDoubleNegativeInfinityJNumber
    read a Float $readFloatJNumber
    read a Float NaN $readFloatNANJNumber
    read a Float Positive Infinity $readFloatPositiveInfinityJNumber
    read a Float Negative Infinity $readFloatNegativeInfinityJNumber
    read a Short $readShortJNumber
    read a Array[Char] $readCharArrayJNumber
    hashCode equals decimal $hashCodeEqualsDecimal
    hashCode equals decimal #2 $hashCodeEqualsDecimal2
    hashCode not equals decimal $hashCodeNotEqualsDecimal
    hashCode not equals decimal #2 $hashCodeNotEqualsDecimal2
    hashCode equals e $hashCodeEqualsE
    hashCode equals e #2 $hashCodeEqualsE2
    hashCode equals e negative $hashCodeEqualsENegative
    hashCode equals e negative #2 $hashCodeEqualsENegative2
    hashCode not equals e negative $hashCodeNotEqualsENegative
    hashCode not equals e negative #2 $hashCodeNotEqualsENegative2
    hashCode equals e positive $hashCodeEqualsEPositive
    hashCode equals e positive #2 $hashCodeEqualsEPositive2
    hashCode not equals e positive $hashCodeNotEqualsEPositive
    hashCode not equals e positive #2 $hashCodeNotEqualsEPositive2
    convert toUnsafe $toUnsafe
    equals $testEquals
    "copy" $testCopy
    "failing copy with NumberFormatException" $testCopyFail
  """

  def readLongJNumber = prop { l: Long =>
    JNumber(l).value must beEqualTo(l.toString)
  }

  def readBigDecimalJNumber = prop { b: BigDecimal =>
    JNumber(b).value must beEqualTo(b.toString())
  }

  def readBigIntJNumber = prop { b: BigInt =>
    JNumber(b).value must beEqualTo(b.toString)
  }

  def readIntJNumber = prop { i: Int =>
    JNumber(i).value must beEqualTo(i.toString)
  }

  def readDoubleJNumber = prop { d: Double =>
    JNumber(d) match {
      case JNull          => JNull must beEqualTo(JNull)
      case JNumber(value) => value must beEqualTo(d.toString)
    }
  }

  def readDoubleNANJNumber = {
    JNumber(Double.NaN) match {
      case JNull => true
      case _     => false
    }
  }

  def readDoublePositiveInfinityJNumber = {
    JNumber(Double.PositiveInfinity) match {
      case JNull => true
      case _     => false
    }
  }

  def readDoubleNegativeInfinityJNumber = {
    JNumber(Double.NegativeInfinity) match {
      case JNull => true
      case _     => false
    }
  }

  def readFloatJNumber = prop { f: Float =>
    JNumber(f) match {
      case JNull          => JNull must beEqualTo(JNull)
      case JNumber(value) => value must beEqualTo(f.toString)
    }
  }

  def readFloatNANJNumber = {
    JNumber(Float.NaN) match {
      case JNull => true
      case _     => false
    }
  }

  def readFloatPositiveInfinityJNumber = {
    JNumber(Float.PositiveInfinity) match {
      case JNull => true
      case _     => false
    }
  }

  def readFloatNegativeInfinityJNumber = {
    JNumber(Float.NegativeInfinity) match {
      case JNull => true
      case _     => false
    }
  }

  def readShortJNumber = prop { s: Short =>
    JNumber(s).value must beEqualTo(s.toString)
  }

  def readCharArrayJNumber = {
    JNumber("34".toCharArray).get.## must beEqualTo(
      JNumber.fromString("34").get.##)
  }

  def hashCodeEqualsDecimal = {
    JNumber.fromString("34").get.## must beEqualTo(
      JNumber.fromString("34.0").get.##)
  }

  def hashCodeEqualsDecimal2 = {
    JNumber.fromString("34").get.## must beEqualTo(
      JNumber.fromString("34.00").get.##)
  }

  def hashCodeNotEqualsDecimal = {
    JNumber.fromString("34").get.## mustNotEqual JNumber
      .fromString("34.01")
      .get
      .##
  }

  def hashCodeNotEqualsDecimal2 = {
    JNumber.fromString("34").get.## mustNotEqual JNumber
      .fromString("34.001")
      .get
      .##
  }

  def hashCodeEqualsE = {
    JNumber.fromString("34e34").get.## must beEqualTo(
      JNumber.fromString("34e034").get.##)
  }

  def hashCodeEqualsE2 = {
    JNumber.fromString("34e34").get.## must beEqualTo(
      JNumber.fromString("34e0034").get.##)
  }

  def hashCodeEqualsENegative = {
    JNumber.fromString("34e-0").get.## must beEqualTo(
      JNumber.fromString("34").get.##)
  }

  def hashCodeEqualsENegative2 = {
    JNumber.fromString("34e-00").get.## must beEqualTo(
      JNumber.fromString("34").get.##)
  }

  def hashCodeNotEqualsENegative = {
    JNumber.fromString("34e-01").get.## mustNotEqual JNumber
      .fromString("34")
      .get
      .##
  }

  def hashCodeNotEqualsENegative2 = {
    JNumber.fromString("34e-001").get.## mustNotEqual JNumber
      .fromString("34")
      .get
      .##
  }

  def hashCodeEqualsEPositive = {
    JNumber.fromString("34e+0").get.## must beEqualTo(
      JNumber.fromString("34").get.##)
  }

  def hashCodeEqualsEPositive2 = {
    JNumber.fromString("34e+00").get.## must beEqualTo(
      JNumber.fromString("34").get.##)
  }

  def hashCodeNotEqualsEPositive = {
    JNumber.fromString("34e+01").get.## mustNotEqual JNumber
      .fromString("34")
      .get
      .##
  }

  def hashCodeNotEqualsEPositive2 = {
    JNumber.fromString("34e+001").get.## mustNotEqual JNumber
      .fromString("34")
      .get
      .##
  }

  def toUnsafe = prop { b: BigDecimal =>
    scalajson.ast.JNumber(b).toUnsafe must beEqualTo(
      scalajson.ast.unsafe.JNumber(b))
  }

  def testEquals = prop { b: BigDecimal =>
    scalajson.ast.JNumber(b) must beEqualTo(scalajson.ast.JNumber(b))
  }

  def testCopy =
    prop { (b1: BigDecimal, b2: BigDecimal) =>
      val asString = b2.toString()
      scalajson.ast.JNumber(b1).copy(value = asString) must beEqualTo(
        scalajson.ast.JNumber(b2))
    }

  def testCopyFail =
    prop { b: BigDecimal =>
      scalajson.ast.JNumber(b).copy(value = "not a number") must throwA(
        new NumberFormatException("not a number"))
    }
}
