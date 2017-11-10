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

  def readLongJNumber = prop { l: Long =>
    JNumber(l).value must beEqualTo(l.toString)
  }

  def readBigDecimalJNumber = prop { b: BigDecimal =>
    JNumber(b).value must beEqualTo(b.toString())
  }

  def readBigIntJNumber = prop { b: BigInt =>
    JNumber(b).value must beEqualTo(b.toString())
  }

  def readIntJNumber = prop { i: Int =>
    JNumber(i).value must beEqualTo(i.toString)
  }

  def readDoubleJNumber = prop { d: Double =>
    JNumber(d).value must beEqualTo(d.toString)
  }

  def readDoubleNANJNumber = {
    JNumber(Double.NaN).value match {
      case "NaN" => true
      case _     => false
    }
  }

  def readDoublePositiveInfinityJNumber = {
    JNumber(Double.PositiveInfinity).value match {
      case "Infinity" => true
      case _          => false
    }
  }

  def readDoubleNegativeInfinityJNumber = {
    JNumber(Double.NegativeInfinity).value match {
      case "-Infinity" => true
      case _           => false
    }
  }

  def readFloatJNumber = prop { f: Float =>
    JNumber(f).value must beEqualTo(f.toString)
  }

  def readShortJNumber = prop { s: Short =>
    JNumber(s).value must beEqualTo(s.toString)
  }

  def readStringJNumber = prop { s: String =>
    JNumber(s).value must beEqualTo(s.toString)
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
      scala.util.Try(BigDecimal(JNumber(s).value)).toOption.isEmpty must beTrue
    }
  }

  def toStandard = prop { b: BigDecimal =>
    JNumber(b).toStandard must beEqualTo(scalajson.ast.JNumber(b))
  }
}
