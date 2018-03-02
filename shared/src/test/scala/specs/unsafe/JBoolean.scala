package specs.unsafe

import specs.Spec
import scalajson.ast.unsafe._

class JBoolean extends Spec {
  "The unsafe.JBoolean value" should {
    "read a Boolean" in {
      forAll { b: Boolean =>
        JBoolean(b).get should be(b)
      }
    }

    "pattern match with JTrue" in {
      forAll { b: Boolean =>
        whenever(b == true) {
          val result = JBoolean(b) match {
            case f @ JTrue => f
          }
          result.get should be(b)
        }
      }
    }

    "pattern match with JTrue and fail with scala.MatchError" in {
      assertThrows[MatchError] {
        JBoolean(true) match {
          case f @ JFalse => f
        }
      }
    }

    "pattern match with JFalse" in {
      forAll { b: Boolean =>
        whenever(b == false) {
          val result = JBoolean(b) match {
            case f @ JFalse => f
          }
          result.get should be(b)
        }
      }
    }

    "pattern match with JFalse and fail with scala.MatchError" in {
      assertThrows[MatchError] {
        JBoolean(false) match {
          case f @ JTrue => f
        }
      }
    }

    "pattern match with JBoolean as true" in {
      forAll { b: Boolean =>
        whenever(b == true) {
          val result = JBoolean(b) match {
            case f @ JBoolean(true) => f
          }
          result.get should be(b)
        }
      }
    }

    "pattern match with JBoolean as true and fail with scala.MatchError" in {
      assertThrows[MatchError] {
        JBoolean(true) match {
          case f @ JBoolean(false) => f
        }
      }
    }

    "pattern match with JBoolean as false" in {
      forAll { b: Boolean =>
        whenever(b == false) {
          val result = JBoolean(b) match {
            case f @ JBoolean(false) => f
          }
          result.get should be(b)
        }
      }
    }

    "pattern match with JBoolean as false and fail with scala.MatchError" in {
      assertThrows[MatchError] {
        JBoolean(false) match {
          case f @ JBoolean(true) => f
        }
      }
    }

    "convert toStandard" in {
      forAll { b: Boolean =>
        JBoolean(b).toStandard should be(scalajson.ast.JBoolean(b))
      }
    }

  }

  "The JTrue value should" should {
    "read a Boolean as true" in {
      forAll { b: Boolean =>
        whenever(b == true) {
          JTrue.get should be(b)
        }
      }
    }
  }

  "The JFalse value should" should {
    "read a Boolean as false" in {
      forAll { b: Boolean =>
        whenever(b == false) {
          JFalse.get should be(b)
        }
      }
    }
  }
}
