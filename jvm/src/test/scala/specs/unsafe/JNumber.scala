package specs.unsafe

import specs.Spec
import scalajson.ast.unsafe._

class JNumber extends Spec {
  def is =
    s2"""
  The unsafe.JNumber value should
    read a Long $readLongJNumber
    read a BigDecimal $readBigDecimalJNumber
    read a BigInt $readBigIntJNumber
    read an Int $readIntJNumber
    read a Double $readDoubleJNumber
    read a Double NaN $readDoubleNANJNumber
    read a Double Positive Infinity $readDoublePositiveInfinityJNumber
    read a Double Negative Infinity $readDoubleNegativeInfinityJNumber
    read a Float $readFloatJNumber
    read a Short $readShortJNumber
    read a String and not fail $readStringJNumber
    read a String and detect non numeric numbers $readStringJNumberDetect
    convert toStandard $toStandard
  """


  def readBigDecimalJNumber = prop { b: BigDecimal =>
    JNumber(b).toBigDecimal must beEqualTo(Option(b))
  }

  def readBigIntJNumber = prop { b: BigInt =>
    JNumber(b).toBigInt must beEqualTo(Option(b))
  }

  def readLongJNumber = prop { l: Long =>
    JNumber(l).toLong must beEqualTo(Option(l))
  }

  def readIntJNumber = prop { i: Int =>
    JNumber(i).toInt must beEqualTo(Option(i))
  }

  def readDoubleJNumber = prop { d: Double =>
    JNumber(d) match {
      case JNull                             => JNull must beEqualTo(JNull)
      case num: scalajson.ast.unsafe.JNumber => num.toDouble must beEqualTo(d)
    }
  }

  def readDoubleNANJNumber = {
    JNumber(Double.NaN) match {
      case JNull => true
      case _ => false
    }
  }

  def readDoublePositiveInfinityJNumber = {
    JNumber(Double.PositiveInfinity) match {
      case JNull => true
      case _ => false
    }
  }

  def readDoubleNegativeInfinityJNumber = {
    JNumber(Double.NegativeInfinity) match {
      case JNull => true
      case _ => false
    }
  }

  def readFloatJNumber = prop { f: Float =>
    JNumber(f) match {
      case JNull                             => JNull must beEqualTo(JNull)
      case num: scalajson.ast.unsafe.JNumber => num.toDouble must beEqualTo(f.toDouble)
    }
  }

  def readShortJNumber = prop { s: Short =>
    JNumber(s).stringValue must beEqualTo(s.toString)
  }

  def readStringJNumber = prop { s: String =>
    JNumber(s).get.stringValue must beEqualTo(s.toString)
  }

  def readStringJNumberDetect = prop { s: String =>
    {
      scala.util
        .Try {
          BigDecimal(s)
        }
        .toOption
        .isEmpty
    } ==> {
      scala.util.Try(BigDecimal(JNumber(s).get.stringValue)).toOption.isEmpty must beTrue
    }
  }

  def toStandard = prop { b: BigDecimal =>
    JNumber(b).toStandard must beEqualTo(scalajson.ast.JNumber(b))
  }
}
