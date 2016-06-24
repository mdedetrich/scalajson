package specs.unsafe

import org.scalacheck.Prop._
import utest._
import Generators._
import specs.UTestScalaCheck

object JValue extends TestSuite with UTestScalaCheck {

  val tests = TestSuite {
    "The JString value should" - {
      "have a bijection with js.Any" - testBijection
    }
  }

  def testBijection =
    forAll { jValue: scala.json.ast.unsafe.JValue =>
      scala.json.ast.unsafe.JValue.fromJsAny(jValue.toJsAny) == Some(jValue)
    }.checkUTest()
}
