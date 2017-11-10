package specs.unsafe

import org.scalacheck.Prop._
import specs.UTestScalaCheck
import utest._

object JBoolean extends TestSuite with UTestScalaCheck {

  val tests = TestSuite {
    "The unsafe.JBoolean value should" - {
      "read a Boolean" - readBooleanJBoolean
      "pattern match with JTrue" - readBooleanJBooleanPatternMatchJTrue
      "pattern match with JTrue and fail with scala.MatchError" -
        readBooleanJBooleanPatternMatchJTrueFail
      "pattern match with JFalse" - readBooleanJBooleanPatternMatchJFalse
      "pattern match with JFalse and fail with scala.MatchError" -
        readBooleanJBooleanPatternMatchJFalseFail
      "pattern match with JBoolean as true" -
        readBooleanJBooleanPatternMatchJBooleanTrue
      "pattern match with JBoolean as true and fail with scala.MatchError" -
        readBooleanJBooleanPatternMatchJBooleanTrueFail
      "pattern match with JBoolean as false" -
        readBooleanJBooleanPatternMatchJBooleanFalse
      "pattern match with JBoolean as false and fail with scala.MatchError" -
        readBooleanJBooleanPatternMatchJBooleanFalseFail
      "The JTrue value should read a Boolean as true" - readBooleanJTrue
      "The JFalse value should read a Boolean as false" - readBooleanJFalse
      "convert to jsAny" - toJsAny
      "convert toStandard" - toStandard
    }
  }

  def readBooleanJBoolean =
    forAll { b: Boolean =>
      scalajson.ast.unsafe.JBoolean(b).get == b
    }.checkUTest()

  def readBooleanJBooleanPatternMatchJBooleanTrue =
    forAll { b: Boolean =>
      {
        b == true
      } ==> {
        val result = scalajson.ast.unsafe.JBoolean(b) match {
          case f @ scalajson.ast.unsafe.JBoolean(true) => f
        }
        result.get == b
      }
    }.checkUTest()

  def readBooleanJBooleanPatternMatchJBooleanTrueFail = {
    try {
      scalajson.ast.unsafe.JBoolean(true) match {
        case f @ scalajson.ast.unsafe.JBoolean(false) => f
      }
    } catch {
      case _: MatchError => true
      case _             => false
    }
  }

  def readBooleanJBooleanPatternMatchJBooleanFalse =
    forAll { b: Boolean =>
      {
        b == false
      } ==> {
        val result = scalajson.ast.unsafe.JBoolean(b) match {
          case f @ scalajson.ast.unsafe.JBoolean(false) => f
        }
        result.get == b
      }
    }.checkUTest()

  def readBooleanJBooleanPatternMatchJBooleanFalseFail = {
    try {
      scalajson.ast.unsafe.JBoolean(false) match {
        case f @ scalajson.ast.unsafe.JBoolean(true) => f
      }
    } catch {
      case _: MatchError => true
      case _             => false
    }
  }

  def readBooleanJBooleanPatternMatchJTrue =
    forAll { b: Boolean =>
      (b == true) ==> {
        val result = scalajson.ast.unsafe.JBoolean(b) match {
          case f @ scalajson.ast.unsafe.JTrue => f
        }
        result.get == b
      }
    }.checkUTest()

  def readBooleanJBooleanPatternMatchJTrueFail = {
    try {
      scalajson.ast.unsafe.JBoolean(true) match {
        case f @ scalajson.ast.unsafe.JFalse => f
      }
    } catch {
      case _: MatchError => true
      case _             => false
    }
  }

  def readBooleanJBooleanPatternMatchJFalse =
    forAll { b: Boolean =>
      (b == false) ==> {
        val result = scalajson.ast.unsafe.JBoolean(b) match {
          case f @ scalajson.ast.unsafe.JFalse => f
        }
        result.get == b
      }
    }.checkUTest()

  def readBooleanJBooleanPatternMatchJFalseFail = {
    try {
      scalajson.ast.unsafe.JBoolean(false) match {
        case f @ scalajson.ast.unsafe.JTrue => f
      }
    } catch {
      case _: MatchError => true
      case _             => false
    }
  }

  def readBooleanJTrue = forAll { b: Boolean =>
    (b == true) ==> {
      scalajson.ast.unsafe.JTrue.get == b
    }
  }

  def readBooleanJFalse = forAll { b: Boolean =>
    (b == false) ==> {
      scalajson.ast.unsafe.JFalse.get == b
    }
  }

  def toJsAny =
    forAll { b: Boolean =>
      scalajson.ast.unsafe.JBoolean(b).toJsAny == b
    }.checkUTest()

  def toStandard =
    forAll { b: Boolean =>
      scalajson.ast.unsafe.JBoolean(b).toStandard == scalajson.ast.JBoolean(b)
    }.checkUTest()
}
