package specs.unsafe

import specs.Spec
import scalajson.ast.unsafe._

class JNumber extends Spec {

  "The unsafe.JNumber value" should {
    "read a Long" in {
      forAll { l: Long =>
        JNumber(l).value should be(l.toString)
      }
    }

    "read a BigDecimal" in {
      forAll { b: BigDecimal =>
        JNumber(b).value should be(b.toString())
      }
    }

    "read a BigInt" in {
      forAll { b: BigInt =>
        JNumber(b).value should be(b.toString())
      }
    }

    "read an Int" in {
      forAll { i: Int =>
        JNumber(i).value should be(i.toString)
      }
    }

    "read a Double" in {
      forAll { d: Double =>
        JNumber(d).value should be(d.toString)
      }
    }

    "read a Double NaN" in {
      JNumber(Double.NaN).value match {
        case "NaN" => true
        case _     => false
      }
    }

    "read a Double Positive Infinity" in {
      JNumber(Double.PositiveInfinity).value match {
        case "Infinity" => true
        case _          => false
      }
    }

    "read a Double Negative Infinity" in {
      JNumber(Double.NegativeInfinity).value match {
        case "-Infinity" => true
        case _           => false
      }
    }

    "read a Float" in {
      forAll { f: Float =>
        JNumber(f).value should be(f.toString)
      }
    }

    "read a Short" in {
      forAll { s: Short =>
        JNumber(s).value should be(s.toString)
      }
    }

    "read a String and not fail" in {
      forAll { s: String =>
        JNumber(s).value should be(s.toString)
      }
    }

    "read a String and detect non numeric numbers" in {
      forAll { s: String =>
        whenever {
          scala.util
            .Try {
              BigDecimal(s)
            }
            .toOption
            .isEmpty
        } {
          scala.util
            .Try(BigDecimal(JNumber(s).value))
            .toOption
            .isEmpty should be(true)
        }
      }
    }

    "convert toStandard" in {
      forAll { b: BigDecimal =>
        JNumber(b).toStandard should be(scalajson.ast.JNumber(b))
      }
    }

  }
}
