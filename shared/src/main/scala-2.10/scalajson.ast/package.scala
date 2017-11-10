package scalajson

import scala.util.matching.Regex

package object ast {

  // Bit flags that are used for storing how a number was constructed

  object NumberFlags {
    @inline private[ast] final def int: Int = 1
    @inline private[ast] final def long: Int = 2
    @inline private[ast] final def bigInt: Int = 4
    @inline private[ast] final def bigDecimal: Int = 8
    @inline private[ast] final def float: Int = 16
    @inline private[ast] final def double: Int = 32

    @inline private[ast] final val intConstructed
      : Int = int | long | bigInt | bigDecimal
    @inline private[ast] final val longConstructed
      : Int = long | bigInt | bigDecimal
    @inline private[ast] final val bigIntConstructed: Int = bigInt
    @inline private[ast] final val bigDecimalConstructed: Int = bigDecimal
    @inline private[ast] final val floatConstructed
      : Int = float | double | bigDecimal
    @inline private[ast] final val doubleConstructed: Int = double | bigDecimal
  }

  /**
    * A regex that will match any valid JSON number for unlimited
    * precision
    */
  protected[ast] val jNumberRegex: Regex =
    """-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?""".r

  /**
    * Finds the hashcode for a numeric JSON string.
    *
    * @author Matthew de Detrich
    * @param value
    * @return
    */
  private[ast] def numericStringHashcode(value: String): Int = {
    var result = 31
    val length = value.length
    var i = 0

    if (value(0) == '-') {
      // Found a negative, increment by one
      result = result * 31 + '-': Int
      i = 1
    }

    var char = value(i)

    // From now on, we can just traverse all the chars

    var negativeFlag = false

    while (i < length) {
      char = value(i)
      // if char is e, lowercase it
      if ((char | 0x20) == 'e') {

        if (value(i + 1) == '-') {
          // Found a negative, increment by one
          i += 1
          char = value(i)
          negativeFlag = true
        } else if (value(i + 1) == '+') {
          // Found a positive, ignore
          i += 1
          char = value(i)
        }

        i += 1
        char = value(i)

        // Need to skip all leading zeroes, possible with e
        while (char == '0' && i < length) {
          i += 1
          if (i != length)
            char = value(i) // Fencepost, possible that this can be last character
        }

        if (i < length) {
          if (negativeFlag) {
            result = result * 31 + '-': Int
          }

          result = 31 * result + 'e': Int
        }
      } else if (char == '.') {

        i += 1
        char = value(i)

        while (char == '0' && i < length) {
          i += 1
          if (i != length)
            char = value(i) // Fencepost, possible that this can be last character
        }

        if (i < length) {
          result = 31 * result + '.': Int // The decimal is not finishing with a 0
        }
      } else {
        result = 31 * result + char: Int
      }

      i += 1
    }
    result
  }

  /** Tests two non-empty strings for equality, assuming both are decimal representations of numbers.
    *
    * Note: if the two strings are NOT decimal representations of numbers, the results of this method are undefined.
    * (It is likely but not guaranteed that the method will return `false` even if the two strings are identical.)
    *
    * Many thanks for @Ichoran (https://github.com/Ichoran) for the implementation

    * @author Rex Kerr
    * @see https://github.com/Ichoran/kse/blob/master/src/main/scala/jsonal/Jast.scala#L683-L917
    */
  private[ast] def numericStringEquals(a: String, b: String): Boolean = {
    var i = 0 // Index for a
    var j = 0 // Index for b
    if (a.length < 1 || b.length < 1) return false
    if (a.charAt(0) == '-') i += 1
    if (b.charAt(0) == '-') j += 1
    if (i >= a.length || j >= b.length) return false
    var ca = a.charAt(i) // Character at index of a
    var cb = b.charAt(j) // Character at index of b
    if (i != j) {
      // Different signs.  They'd better both be zero
      return {
        if (ca == '0' && cb == '0') {
          while (i < a.length - 1 && (ca == '0' || ca == '.')) {
            i += 1; ca = a.charAt(i)
          }
          while (j < b.length - 1 && (cb == '0' || cb == '.')) {
            j += 1; cb = b.charAt(j)
          }
          ((i == a.length - 1 && ca == '0') || (ca | 0x20) == 'e') &&
          ((j == b.length - 1 && cb == '0') || (cb | 0x20) == 'e')
        } else false
      }
    }
    var pa = 0 // Decimal point position for a
    var pb = 0 // Decimal point position for b
    if (ca == '0') {
      pa = -1
      if (i < a.length - 1) {
        val x = a.charAt(i + 1)
        if ((x | 0x20) != 'e') {
          if (x != '.') return false
          else if (i < a.length - 2) {
            i += 2
            ca = a.charAt(i)
            while (ca == '0' && i < a.length - 1) {
              i += 1; ca = a.charAt(i); pa -= 1
            }
          }
        }
      }
    }
    if (cb == '0') {
      pb = -1
      if (j < b.length - 1) {
        val y = b.charAt(j + 1)
        if ((y | 0x20) != 'e') {
          if (y != '.') return false
          else if (j < b.length - 2) {
            j += 2
            cb = b.charAt(j)
            while (cb == '0' && j < b.length - 1) {
              j += 1; cb = b.charAt(j); pb -= 1
            }
          }
        }
      }
      // Might both be zero.  Check!  (Can ignore exponents when both values are zero.)
      if (ca < '1' || ca > '9') return cb < '1' || cb > '9'
    }
    var fa = pa < 0 // Found a's decimal point?
    var fb = pb < 0 // Found b's decimal point?
    while (ca == cb && (ca | 0x20) != 'e' && i < a.length - 1 &&
           j < b.length - 1) {
      i += 1
      ca = a.charAt(i)
      if (!fa) {
        pa += 1
        if (ca == '.') {
          fa = true
          if (i < a.length - 1) {
            i += 1
            ca = a.charAt(i)
          }
        }
      }
      j += 1
      cb = b.charAt(j)
      if (!fb) {
        pb += 1
        if (cb == '.') {
          fb = true
          if (j < b.length - 1) {
            j += 1
            cb = b.charAt(j)
          }
        }
      }
    }
    if (ca != cb && (ca | 0x20) != 'e' && (cb | 0x20) != 'e') return false // Digits simply disagree
    // Capture any trailing zeros
    if (!(i >= a.length - 1 && j >= b.length - 1)) {
      if (j >= b.length - 1 || (cb | 0x20) == 'e') {
        if (j >= b.length - 1) {
          if (!fb) pb += 1
          // Advance a off the end, make sure it's all zeros
          i += 1
          ca = a.charAt(i)
          if (!fa) {
            pa += 1
            if (ca == '.') {
              fa = true
              if (i < a.length - 1) {
                i += 1
                ca = a.charAt(i)
              }
            }
          }
        }
        while (i < a.length - 1 && ca == '0') {
          i += 1
          ca = a.charAt(i)
          if (!fa) {
            pa += 1
            if (ca == '.') {
              fa = true
              if (i < a.length - 1) {
                i += 1
                ca = a.charAt(i)
              }
            }
          }
        }
        if (ca >= '1' && ca <= '9') return false // Extra digits on a
      } else if (i >= a.length - 1 || (ca | 0x20) == 'e') {
        if (i >= a.length - 1) {
          if (!fa) pa += 1
          // Advance b off the end, make sure it's all zeros
          j += 1
          cb = b.charAt(j)
          if (!fb) {
            pb += 1
            if (cb == '.') {
              fb = true
              if (j < b.length - 1) {
                j += 1
                cb = b.charAt(j)
              }
            }
          }
        }
        while (j < b.length - 1 && cb == '0') {
          j += 1
          cb = b.charAt(j)
          if (!fb) {
            pb += 1
            if (cb == '.') {
              fb = true
              if (j < b.length - 1) {
                j += 1
                cb = b.charAt(j)
              }
            }
          }
        }
        if (cb >= '1' && cb <= '9') return false // Extra digits on b
      }
    }
    if (pa > 0) pa -= 1
    if (pb > 0) pb -= 1
    if ((ca | 0x20) == 'e' && (cb | 0x20) == 'e') {
      if (i >= a.length - 1) return false
      i += 1
      ca = a.charAt(i)
      val nega =
        if (ca == '-' || ca == '+') {
          val ans = ca == '-'
          if (i >= a.length - 1) return false
          i += 1
          ca = a.charAt(i)
          ans
        } else false
      if (j >= b.length - 1) return false
      j += 1
      cb = b.charAt(j)
      val negb =
        if (cb == '-' || cb == '+') {
          val ans = cb == '-'
          if (j >= b.length - 1) return false
          j += 1
          cb = b.charAt(j)
          ans
        } else false
      if (a.length - i < 10 && b.length - j < 10) {
        val ea = a.substring(i).toInt
        val eb = b.substring(j).toInt
        (if (negb) pb - eb else pb + eb) == (if (nega) pa - ea else pa + ea)
      } else if (a.length - i < 18 && b.length - j < 18) {
        val ea = a.substring(i).toLong
        val eb = b.substring(j).toLong
        (if (negb) pb - eb else pb + eb) == (if (nega) pa - ea else pa + ea)
      } else {
        val ea = BigInt(a.substring(i))
        val eb = BigInt(b.substring(j))
        (if (negb) pb - eb else pb + eb) == (if (nega) pa - ea else pa + ea)
      }
    } else if ((ca | 0x20) == 'e') {
      if (i >= a.length - 1) return false
      i += 1
      ca = a.charAt(i)
      val nega =
        if (ca == '-' || ca == '+') {
          val ans = ca == '-'
          if (i >= a.length - 1) return false
          i += 1
          ca = a.charAt(i)
          ans
        } else false
      if (a.length - i < 10) {
        val ea = a.substring(i).toInt
        pb == (if (nega) pa - ea else pa + ea)
      } else if (a.length - i < 18) {
        val ea = a.substring(i).toLong
        pb == (if (nega) pa - ea else pa + ea)
      } else {
        val ea = BigInt(a.substring(i))
        pb == (if (nega) pa - ea else pa + ea)
      }
    } else if ((cb | 0x20) == 'e') {
      if (j >= b.length - 1) return false
      j += 1
      cb = b.charAt(j)
      val negb =
        if (cb == '-' || cb == '+') {
          val ans = cb == '-'
          if (j >= b.length - 1) return false
          j += 1
          cb = b.charAt(j)
          ans
        } else false
      if (b.length - j < 10) {
        val eb = b.substring(j).toInt
        pa == (if (negb) pb - eb else pb + eb)
      } else if (b.length - j < 18) {
        val eb = b.substring(j).toLong
        pa == (if (negb) pb - eb else pb + eb)
      } else {
        val eb = BigInt(b.substring(j))
        pa == (if (negb) pb - eb else pb + eb)
      }
    } else pa == pb
  }
}
