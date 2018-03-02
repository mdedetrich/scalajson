# ScalaJSON

[![Join the chat at https://gitter.im/mdedetrich/scalajson](https://badges.gitter.im/mdedetrich/scalajson.svg)](https://gitter.im/mdedetrich/scalajson?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![codecov.io](http://codecov.io/github/mdedetrich/scalajson/coverage.svg?branch=master)](http://codecov.io/github/mdedetrich/scalajson?branch=master)
[![Build Status](https://travis-ci.org/mdedetrich/scalajson.svg?branch=master)](https://travis-ci.org/mdedetrich/scalajson)
[![Scala.js](https://www.scala-js.org/assets/badges/scalajs-0.6.17.svg)](https://www.scala-js.org)

ScalaJSON library, currently provides two minimal implementations of [JSON](https://en.wikipedia.org/wiki/JSON) AST, one that is designed for
typical use and another that is designed for performance/corner cases.

## Usage

Built for Scala 2.10.x, 2.11.x, 2.12.x, and 2.13.x milestones.

```sbt
"org.scala-lang.platform" %% "scalajson" % "1.0.0-M4"
```

If you are using Scala.js, you need to do

```sbt
"org.scala-lang.platform" %%% "scalajson" % "1.0.0-M4"
```

## Standard AST
Implementation is in `scalajson.ast.JValue`

### Goals
- Fully immutable (all underlying collections/types used are immutable)
- `constant`/`effective constant` lookup time for `scalajson.ast.JArray`/`scalajson.ast.JObject`
- Adherence to the typical use for the [JSON](https://en.wikipedia.org/wiki/JSON) standard.
    - Number representation for `scalajson.ast.JNumber` is a `String` which checks if its a valid JSON representation
      of a number (http://stackoverflow.com/a/13502497/1519631)
      - Equals will properly detect if two numbers are equal, i.e. `scalajson.ast.JNumber("34.00") == scalajson.ast.JNumber("34")`
      - Hashcode has been designed to provide consistent hash for numbers of unlimited precision.
      - If you construct a JNumber with `Float.NaN`/`Float.PositiveInfinity`/`Float.NegativeInfinity`/`Double.NaN`/`Double.PositiveInfinity`/`Double.NegativeInfinity` it will return a `JNull`
      - You can construct an unlimited precision number using a string, i.e. `JNumber("34324")`. Returns an `Option[JNumber]` (will return `None` if `String` isn't a valid number)
        - Note that this doesn't work for Scala 2.10 due to a restriction with how private constructor case classes are handled. For this reason a `JNumber.fromString` method is provided which compiles on all platforms and scala versions
    - `scalajson.ast.JObject` is an actual `Map[String,JValue]`. This means that it doesn't handle duplicate keys for a `scalajson.ast.JObject`,
    nor does it handle key ordering.
    - `scalajson.ast.JArray` is an `Vector`.
- Library does not allow invalid JSON in the representation and hence we can guarantee that a `scalajson.ast.JValue` will 
always contain a valid structure that can be serialized/rendered into [JSON](https://en.wikipedia.org/wiki/JSON). 
  - Note that you can lose precision when using `scalajson.ast.JNumber` in `Scala.js` (see `Scala.js` 
section for more info).
  - The `.copy` method of `scalajson.ast.JNumber` has been overridden to make sure you can't replace the internal `String`
  with an incorrect number
    - Will throw a `NumberFormatException` if you use `.copy` with an invalid JSON number
- Due to the above, has properly implemented deep equality for all types of `scalajson.ast.JValue`

## Unsafe AST
Implementation is in `scalajson.unsafe.JValue`

### Goals
- Uses the best performing datastructure's for high performance/low memory usage in construction of a `unsafe.JValue`
    - `scalajson.ast.unsafe.JArray` stored as an `Array` for JVM and `js.Array` for Scala.js
    - `scalajson.ast.unsafe.JObject` stored as an `Array` for JVM and `js.Array` for Scala.js
    - `scalajson.ast.unsafe.JNumber` stored as a `String`
- Doesn't use `Scala`'s `stdlib` collection's library
- Defer all runtime errors. We don't throw errors if you happen to provide Infinity/NaN or other invalid data.
- Allow duplicate and ordered keys for a `scalajson.ast.unsafe.JObject`.
- Allow any length/precision of numbers for `scalajson.ast.unsafe.JNumber` since its represented as a `String`
  - Equals/hashcode only checks for `String` equality, not number equality.
    - Means that `scalajson.ast.unsafe.JNumber("34.00")` is not equal to `scalajson.ast.unsafe.JNumber("34")`
- This means that `scalajson.ast.unsafe.JValue` can represent everything that can
can be considered valid under the official [JSON spec](https://www.ietf.org/rfc/rfc4627.txt), even if its not considered sane (i.e.
duplicate keys for a `scalajson.ast.unsafe.JObject`).
  - Also means it can hold invalid data, due to not doing runtime checks
- Is referentially transparent in regards to `String` -> `scalajson.ast.unsafe.JValue` -> `String` since `scalajson.ast.unsafe.JObject` 
  preserves ordering/duplicate keys
- Implements structural equality for both `hashCode` and `equals`. If you need reference equality
  you can use `eq` and if you need reference `hashCode` you can use `.value.hashCode`. Also note that for
  deep comparison is used both `hashCode` and `equals`.

## Conversion between scalajson.JValue and scalajson.ast.unsafe.JValue

Any `scalajson.ast.JValue` implements a conversion to `scalajson.ast.unsafe.JValue` with a `toUnsafe` method and vice versa with a
`toStandard` method. These conversion methods have been written to be as fast as possible. Since there are  many `immutable.Map`
implementations that can satisfy a `scalaJson.JValue` (with varying properties), the `.toStandard` method allows you to specify what type of
`immutable.Map` your `scalajson.ast.JValue` will have, i.e.

```scala
import scalajson.ast.unsafe._
import scala.collection.immutable.ListMap

val jsonObject = JObject(
  Array(
    JField("a" -> JString("a")),
    JField("b" -> JString("b")),
    JField("c" -> JString("c")),
    JField("d" -> JString("d")),
    JField("e" -> JString("e"))
  )
)

// This is the default, will create an immutable Map. Note that it will not maintain key ordering for sizes > 4
jsonObject.toStandard[Map]
// Convert to a ListMap, this will maintain key ordering but lose effectively constant lookup by key
jsonObject.toStandard[ListMap]
```

Similarly, converting from a `scalajson.ast.JObject` to a `scalajson.ast.unsafe.JObject` will produce 
a `scalajson.ast.unsafe.JObject` with an undefined ordering for its internal `Array`/`js.Array` representation
if the `scalajson.ast.JObject` is implemented using an  `immutable.Map`. This is because an `immutable.Map` has no
predefined ordering. If you wish to provide ordering, you will either need to write your own custom conversion to handle
this case.

Duplicate keys will also be removed in an undefined manner.

There are some other peculiarities when converting between the two AST's. When converting a `scalajson.ast.unsafe.JNumber` to a 
`scalajson.ast.JNumber`, it is possible for this to fail at runtime (since the internal representation of 
`scalajson.ast.unsafe.JNumber` is a `String` and it doesn't have a runtime check). It is up to the caller on how to handle this error (and when), 
a runtime check is deliberately avoided on our end for performance reasons.

Do note that according to the JSON spec, whether to order keys for a `JObject` is not specified. Also note that `Map` 
disregards ordering for equality, however `Array`/`js.Array` equality takes ordering into account.

## Number conversions
ScalaJSON `JNumber` provides conversions to various number types with the following conventions

* `toInt`: Safe conversion to `Int` which accounts for values such as `1.0` and `100.00e-2` (which both evaluate to `1`).
Also safely detects over/underflow.
* `toLong`: Safe conversion to `Long` which accounts for values such as `1.0` and `100.00e-2` (which both evaluate to `1`).
Also  safely detects over/underflow.
* `toDouble`: Converts to a `Double` assuming the same semantics of `Double` (i.e. precision loss is expected).
* `toFloat`: Converts to a `Float` assuming the same semantics of `Float` (i.e. precision loss is expected).
* `toBigInt`: Converts to a `BigInt` which accounts for values such as `1.0` and `100.00e-2` (which evaluates to `1`).
Can construct a `BigInt`for as much as memory as the system has (if your system runs out of memory this is considered
undefined behaviour).
* `toBigDecimal`: Converts to a `BigDecimal` with all of the caveats of `BigDecimal` construction. The `BigDecimal` is
constructed with `MathContext.UNLIMITED` precision.

With the `.toFloat` and `.toDouble` methods, if you don't want any loss in precision, its advisable to convert to
`BigDecimal` first and then work from there, i.e. when working with `Decimal`/`Float`, its implied that you will
have loss of precision.

Remember that in all cases if these methods are not applicable, you can always use the `.value` field to get the
original string representation of the number.

## Scala.js
ScalaJSON also provides support for [Scala.js](https://github.com/scala-js/scala-js).
The usage of Scala.js mirrors the usage of Scala on the JVM however Scala.js also implements
a `.toJsAny` method which allows you to convert any
`scalajson.ast.JValue`/`scalajson.ast.unsafe.JValue` to a Javascript value in `Scala.js`.

Note that, since a `scalajson.ast.JNumber`/`scalajson.ast.unsafe.JNumber` is unlimited
precision (represented internally as a `String`), calls to `.toJsAny` can lose precision on the
underlying number (numbers in Javascript are represented as double precision floating point number).
You can use the `.value` method on a `scalajson.ast.JNumber`/`scalajson.ast.unsafe.JNumber` to
get the raw string value as a solution to this problem.

Further, `toFloat` on `JNumber` (see [Number Conversions](#number-conversions) ) can have different semantics on Scala.js, depending on whether you have
strict-floats enabled in your application. Please see the [Scala.js semantics page](https://www.scala-js.org/doc/semantics.html)
for more information.

## jNumberRegex
`scalajson.JNumber` uses `jNumberRegex` to validate whether a number is a valid
JSON number. One can use `jNumberRegex` explicitly if you want to use the validation that
is used by `scalajson.JNumber` (for example, you may want to validate proper numbers
before creating a `scalajson.unsafe.JNumber`).

```scala
import scalajson.jNumberRegex

"3535353" match {
  case jNumberRegex(_ *) => true
  case _ => false
}
```

## Code of Conduct
ScalaJSON uses the [Scala Code of Conduct](https://www.scala-lang.org/conduct.html)
for all communication and discussion. This includes both GitHub, Gitter chat and
other more direct lines of communication such as email.

## Code formatting

The project is formatted using [scalafmt](https://github.com/olafurpg/scalafmt). Please run `scalafmt`
in SBT before committing any changes
