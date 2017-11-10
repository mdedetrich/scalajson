package scalajson.ast

import org.scalameter.{Bench, Gen}

object JNumberConversionBenchmark extends Bench.ForkedTime {
  def intString: Gen[String] =
    for {
      size <- Gen.range("seed")(300000, 1500000, 300000)
    } yield {
      size.toString
    }

  def floatString: Gen[String] =
    for {
      a <- Gen.range("seed")(300000, 1500000, 300000)
      b <- Gen.range("seed")(300000, 1500000, 300000)
    } yield {
      s"$a.$b".toFloat.toString
    }

  private val intConstructedFlag = NumberFlags.intConstructed
  private val floatConstructedFlag = NumberFlags.floatConstructed

  performance of "intBitFlagCheckSuccess" in {
    using(intString) in { value: String =>
      if ((intConstructedFlag & NumberFlags.int) == NumberFlags.int)
        Some(value.toInt)
      else {
        try {
          val asInt = value.toInt
          if (BigInt(value) == BigInt(asInt))
            Some(asInt)
          else
            None
        } catch {
          case _: NumberFormatException => None
        }
      }
    }
  }

  performance of "intManualCheckSuccess" in {
    using(intString) in { value: String =>
      try {
        val asInt = value.toInt
        if (BigInt(value) == BigInt(asInt))
          Some(asInt)
        else
          None
      } catch {
        case _: NumberFormatException => None
      }
    }
  }

  performance of "floatBitFlagCheckSuccess" in {
    using(floatString) in { value: String =>
      if ((floatConstructedFlag & NumberFlags.float) == NumberFlags.float)
        Some(value.toFloat)
      else {
        try {
          val asFloat = value.toFloat
          if (BigDecimal(value) == BigDecimal(asFloat.toDouble))
            Some(asFloat)
          else
            None
        } catch {
          case _: NumberFormatException => None
        }

      }
    }
  }

  performance of "floatManualCheckSuccess" in {
    using(floatString) in { value: String =>
      try {
        val asFloat = value.toFloat
        if (BigDecimal(value) == BigDecimal(asFloat.toDouble))
          Some(asFloat)
        else
          None
      } catch {
        case _: NumberFormatException => None
      }

    }
  }
}
