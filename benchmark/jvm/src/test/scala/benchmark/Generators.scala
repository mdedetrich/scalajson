package benchmark

import org.scalameter._

import scala.collection.immutable.VectorMap
import scalajson.ast

object Generators {
  def jBoolean: Gen[ast.JBoolean] =
    for {
      size <- Gen.range("seed")(300000, 1500000, 300000)
    } yield {
      if (size % 2 == 0)
        ast.JBoolean(true)
      else
        ast.JBoolean(false)
    }

  def jString: Gen[ast.JString] =
    for {
      size <- Gen.range("seed")(300000, 1500000, 300000)
    } yield {
      ast.JString(size.toString)
    }

  def jNumber: Gen[ast.JNumber] =
    for {
      size <- Gen.range("seed")(300000, 1500000, 300000)
      size2 <- Gen.range("seed")(300000, 1500000, 300000)
    } yield {
      ast.JNumber.fromString(s"$size.$size2").get
    }

  def jArray: Gen[ast.JArray] =
    for {
      size <- Gen.range("seed")(0, 10, 1)
      randomJValue <- jValue
    } yield {

      var index = 0
      val b = Vector.newBuilder[ast.JValue]
      while (index < size) {
        b += randomJValue
        index += 1
      }
      ast.JArray(b.result())
    }

  def jObject: Gen[ast.JObject] =
    for {
      size <- Gen.range("seed")(0, 10, 1)
      string <- Gen.range("seed")(300000, 1500000, 300000).map {
        _.toString
      }
      randomJValue <- jValue
    } yield {

      val b = VectorMap.newBuilder[String, ast.JValue]

      (0 until size).foreach { _ =>
        b += ((string, randomJValue))
      }

      ast.JObject(b.result())
    }

  def jValue: Gen[ast.JValue] =
    Gen.range("JValue type")(300000, 1500000, 300000).flatMap { seed =>
      seed % 5 match {
        case 0 => jBoolean.asInstanceOf[Gen[ast.JValue]]
        case 1 => jString.asInstanceOf[Gen[ast.JValue]]
        case 2 => jNumber.asInstanceOf[Gen[ast.JValue]]
        case 3 => jArray.asInstanceOf[Gen[ast.JValue]]
        case 4 => jString.asInstanceOf[Gen[ast.JValue]]
      }
    }
}
