/**
  * Shamelessly taken from
  * https://github.com/jeffmay/play-json-ops/tree/master/playJsonTests/src/main/scala/play/api/libs/json/scalacheck
  */
package specs

import java.math.MathContext

import org.scalacheck.Arbitrary._
import org.scalacheck.Gen._
import org.scalacheck.Shrink._
import org.scalacheck.{Arbitrary, Gen, Shrink}

object Generators {

  /**
    * The maximum number of fields of a [[scalajson.ast.JObject]] or elements of a [[scalajson.ast.JArray]] to construct when
    * generating one of these nested [[scalajson.ast.JValue]]s.
    */
  def defaultMaxWidth: Width = Width(2)

  /**
    * The maximum number of levels deep where nested values ([[JObject]]s or [[JArray]]s) can be generated.
    *
    * In other words:
    * - A depth of 0 generates only primitive [[JValue]]s
    * - A depth of 1 generates any type of [[JValue]] where all nested values contain only primitive [[JValue]]s.
    * - A depth of n generates any type of [[JValue]] where all nested values contain [[JValue]]s with a depth of n - 1
    */
  def defaultMaxDepth: Depth = Depth(2)

  implicit def arbJsValue(
      implicit maxDepth: Depth = defaultMaxDepth,
      maxWidth: Width = defaultMaxWidth): Arbitrary[scalajson.ast.JValue] =
    Arbitrary(genJsValue)

  implicit def arbJsObject(
      implicit maxDepth: Depth = defaultMaxDepth,
      maxWidth: Width = defaultMaxWidth): Arbitrary[scalajson.ast.JObject] =
    Arbitrary(genJsObject)

  implicit def arbJsArray(
      implicit maxDepth: Depth = defaultMaxDepth,
      maxWidth: Width = defaultMaxWidth): Arbitrary[scalajson.ast.JArray] =
    Arbitrary(genJsArray)

  implicit def arbJsString(implicit arbString: Arbitrary[String])
    : Arbitrary[scalajson.ast.JString] = Arbitrary {
    arbString.arbitrary map scalajson.ast.JString
  }

  implicit def arbJsNumber: Arbitrary[scalajson.ast.JNumber] = Arbitrary {
    genSafeBigDecimal map (x => scalajson.ast.JNumber(x))
  }

  implicit def arbJsBoolean: Arbitrary[scalajson.ast.JBoolean] = Arbitrary {
    Gen.oneOf(true, false) map (x => scalajson.ast.JBoolean(x))
  }

  /**
    * The Jaxson parser has trouble with very large exponents of BigDecimals.
    *
    * I couldn't quite pin the problem down to precision, scale, or both, so
    */
  def genSafeBigDecimal: Gen[BigDecimal] = {
    def chooseBigInt: Gen[BigInt] =
      sized((s: Int) => choose(-s, s)) map (x => BigInt(x))
    def genBigInt: Gen[BigInt] = Gen.frequency(
      (10, chooseBigInt),
      (1, BigInt(0)),
      (1, BigInt(1)),
      (1, BigInt(-1)),
      (1, BigInt(Int.MaxValue) + 1),
      (1, BigInt(Int.MinValue) - 1),
      (1, BigInt(Long.MaxValue)),
      (1, BigInt(Long.MinValue)),
      (1, BigInt(Long.MaxValue) + 1),
      (1, BigInt(Long.MinValue) - 1)
    )
    val mc = MathContext.DECIMAL128
    for {
      x <- genBigInt
      // Generates numbers outside the range of a Double without breaking the Jaxson parser.
      // I couldn't find the true source of the exception in Jaxson, but this should still cover most needs.
      scale <- Gen.chooseNum(10000, Int.MaxValue)
    } yield BigDecimal(x, scale, mc)
  }

  /**
    * Generates non-nested [[scalajson.ast.JValue]]s (ie. not [[scalajson.ast.JArray]] or [[scalajson.ast.JObject]]).
    */
  def genJsPrimitive: Gen[scalajson.ast.JValue] = {
    val genPrims: List[Gen[scalajson.ast.JValue]] = List(
      arbJsBoolean.arbitrary,
      arbJsNumber.arbitrary,
      arbJsString.arbitrary,
      Gen.const(scalajson.ast.JNull)
    )

    // A goofy way to match the signature of this method that I want, but it works
    Gen.oneOf(genPrims.head, genPrims.tail.head, genPrims.tail.tail: _*)
  }

  /**
    * Generates a primitive or nested [[scalajson.ast.JValue]] up to the specified depth and width
    *
    * @param maxDepth see [[defaultMaxDepth]] (cannot be less than 0)
    * @param maxWidth see [[defaultMaxWidth]] (cannot be less than 0)
    */
  def genJsValue(
      implicit maxDepth: Depth = defaultMaxDepth,
      maxWidth: Width = defaultMaxWidth): Gen[scalajson.ast.JValue] = {
    if (maxDepth === 0) genJsPrimitive
    else
      Gen.oneOf(
        genJsPrimitive,
        // The Scala compiler has a bug with AnyVal, where it favors implicits in the outer scope
        genJsArray(maxDepth, maxWidth),
        genJsObject(maxDepth, maxWidth)
      )
  }

  /**
    * Generates a nested array at the specified depth and width.
    *
    * @note the arrays may contain mixed type values at different depths, but never deeper than the [[defaultMaxDepth]].
    *
    * @param maxDepth see [[defaultMaxDepth]] (cannot be less than 1)
    * @param maxWidth see [[defaultMaxWidth]] (cannot be less than 1)
    */
  def genJsArray(
      implicit maxDepth: Depth = defaultMaxDepth,
      maxWidth: Width = defaultMaxWidth): Gen[scalajson.ast.JArray] =
    Gen.containerOfN[Vector, scalajson.ast.JValue](
      maxWidth,
      genJsValue(maxDepth - 1, maxWidth)) map { scalajson.ast.JArray.apply }

  /**
    * Generates a valid field name where the first character is alphabetical and the remaining chars
    * are alphanumeric.
    */
  def genFieldName: Gen[String] = Gen.identifier

  def genFields(implicit maxDepth: Depth = defaultMaxDepth,
                maxWidth: Width = defaultMaxWidth)
    : Gen[(String, scalajson.ast.JValue)] = {
    // The Scala compiler has a bug with AnyVal, where it favors implicits in the outer scope
    Gen.zip(genFieldName, genJsValue(maxDepth, maxWidth))
  }

  /**
    * Generates a nested array at the specified depth and width.
    *
    * @param maxDepth see [[defaultMaxDepth]] (cannot be less than 1)
    * @param maxWidth see [[defaultMaxWidth]] (cannot be less than 1)
    */
  def genJsObject(
      implicit maxDepth: Depth = defaultMaxDepth,
      maxWidth: Width = defaultMaxWidth): Gen[scalajson.ast.JObject] = {
    for {
      fields <- Gen
        .listOfN(maxWidth, genFields(maxDepth - 1, maxWidth))
        .map(_.toMap)
    } yield scalajson.ast.JObject(fields)
  }

  // Shrinks for better error output

  implicit val shrinkJsArray: Shrink[scalajson.ast.JArray] = Shrink { arr =>
    val stream: Stream[scalajson.ast.JArray] = shrink(arr.value) map { x =>
      scalajson.ast.JArray(x)
    }
    stream
  }

  implicit val shrinkJsObject: Shrink[scalajson.ast.JObject] = Shrink { obj =>
    val stream: Stream[scalajson.ast.JObject] = shrink(obj.value) map {
      fields =>
        scalajson.ast.JObject(fields)
    }
    stream
  }

  implicit val shrinkJsValue: Shrink[scalajson.ast.JValue] = Shrink {
    case array: scalajson.ast.JArray => shrink(array)
    case obj: scalajson.ast.JObject => shrink(obj)
    case scalajson.ast.JString(str) => shrink(str) map scalajson.ast.JString
    case scalajson.ast.JNumber(num) =>
      shrink(num) map (x => scalajson.ast.JNumber.fromString(x).get)
    case scalajson.ast.JNull | scalajson.ast.JBoolean(_) =>
      Stream.empty[scalajson.ast.JValue]
  }

}
