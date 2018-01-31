/*
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2017-2018
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.techcode.streamy.util.json

import java.io.InputStream

import akka.util.ByteString

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object Json {

  /**
    * Construct a new JsObject, with the order of fields in the Seq.
    */
  def obj(fields: (String, Json)*): JsObject = JsObject(mutable.AnyRefMap(fields: _*))

  /**
    * Construct a new JsArray with given values.
    */
  def arr(values: Json*): JsArray = JsArray(mutable.ArrayBuffer(values: _ *))

  /**
    * Create a new json object builder.
    *
    * @return json object builder.
    */
  def objectBuilder(): JsObjectBuilder = JsObjectBuilder()

  /**
    * Create a new json array builder.
    *
    * @return json array builder.
    */
  def arrayBuilder(): JsArrayBuilder = JsArrayBuilder()

  /**
    * Parses a bytestring representing a Json input, and returns it as a [[Json]].
    *
    * @param data the bytestring to parse.
    */
  @inline def parse(data: ByteString): Either[Throwable, Json] = JsonJackson.parse(data)

  /**
    * Parses a string representing a Json input, and returns it as a [[Json]].
    *
    * @param data the string to parse.
    */
  @inline def parse(data: Array[Byte]): Either[Throwable, Json] = JsonJackson.parse(data)

  /**
    * Parses a string representing a Json input, and returns it as a [[Json]].
    *
    * @param input the string to parse.
    */
  @inline def parse(input: String): Either[Throwable, Json] = JsonJackson.parse(input)

  /**
    * Parses a stream representing a Json input, and returns it as a [[Json]].
    *
    * @param stream the InputStream to parse.
    */
  @inline def parse(stream: InputStream): Either[Throwable, Json] = JsonJackson.parse(stream)

  /**
    * Converts a [[Json]] to its string representation.
    *
    * @return a string with the json representation.
    */
  @inline def stringify(json: Json): String = JsonJackson.stringify(json, escapeNonASCII = false)

  /**
    * Converts a [[Json]] to its string representation.
    *
    * @return all non-ascii characters escaped.
    */
  @inline def asciiStringify(json: Json): String = JsonJackson.stringify(json, escapeNonASCII = true)

}

/**
  * Generic json value.
  */
sealed trait Json {

  /**
    * Evaluate the given path in the given json value.
    *
    * @param path path to used.
    * @return value if founded, otherwise [[None]].
    */
  def evaluate(path: JsonPointer): Option[Json] = path(this)

  /**
    * Patch given json value by applying all json operations in a transactionnal way.
    *
    * @param op  operation to apply first.
    * @param ops seq of operations to apply.
    * @return json value patched or original.
    */
  def patch(op: JsonOperation, ops: JsonOperation*): Option[Json] = {
    var result: Option[Json] = op(this)
    ops.takeWhile(_ => result.isDefined).foreach(op => result = op(result.get))
    result
  }

  /**
    * Patch given json value by applying all json operations in a transactionnal way.
    *
    * @param ops seq of operations to apply.
    * @return json value patched or original.
    */
  def patch(ops: Seq[JsonOperation]): Option[Json] = {
    var result: Option[Json] = Some(this)
    ops.takeWhile(_ => result.isDefined).foreach(op => result = op(result.get))
    result
  }

  /**
    * Deep merge this object with another one.
    *
    * @param other other json value.
    * @return deep merged json value or none if failed.
    */
  def deepMerge(other: Json): Option[Json] = None

  /**
    * Returns true if the json value is a json object.
    *
    * @return true if the json value is a json object, otherwise false.
    */
  def isObject: Boolean = false

  /**
    * Returns true if the json value is a json array.
    *
    * @return true if the json value is a json array, otherwise false.
    */
  def isArray: Boolean = false

  /**
    * Returns true if the json value is a json bytes.
    *
    * @return true if the json value is a json bytes, otherwise false.
    */
  def isBytes: Boolean = false

  /**
    * Returns true if the json value is a json boolean.
    *
    * @return true if the json value is a json boolean, otherwise false.
    */
  def isBoolean: Boolean = false

  /**
    * Returns true if the json value is a json string.
    *
    * @return true if the json value is a json string, otherwise false.
    */
  def isString: Boolean = false

  /**
    * Returns true if the json value is a json number.
    *
    * @return true if the json value is a json number, otherwise false.
    */
  def isNumber: Boolean = false

  /**
    * Returns true if the json value is a json null.
    *
    * @return true if the json value is a json null, otherwise false.
    */
  def isNull: Boolean = false

  /**
    * Returns true if the json value is a json int.
    *
    * @return true if the json value is a json int, otherwise false.
    */
  def isInt: Boolean = false

  /**
    * Returns true if the json value is a json long.
    *
    * @return true if the json value is a json long, otherwise false.
    */
  def isLong: Boolean = false

  /**
    * Returns true if the json value is a json double.
    *
    * @return true if the json value is a json double, otherwise false.
    */
  def isDouble: Boolean = false

  /**
    * Returns true if the json value is a json float.
    *
    * @return true if the json value is a json float, otherwise false.
    */
  def isFloat: Boolean = false

  /**
    * Returns current json value as json object.
    *
    * @return current json value as json object if possible, otherwise [[None]].
    */
  def asObject: Option[JsObject] = None

  /**
    * Returns current json value as json array.
    *
    * @return current json value as json array if possible, otherwise [[None]].
    */
  def asArray: Option[JsArray] = None

  /**
    * Returns current json value as byte string.
    *
    * @return current json value as byte string if possible, otherwise [[None]].
    */
  def asBytes: Option[ByteString] = None

  /**
    * Returns current json value as json boolean.
    *
    * @return current json value as json boolean if possible, otherwise [[None]].
    */
  def asBoolean: Option[Boolean] = None

  /**
    * Returns current json value as json string.
    *
    * @return current json value as json string if possible, otherwise [[None]].
    */
  def asString: Option[String] = None

  /**
    * Returns current json value as json number.
    *
    * @return current json value as json number if possible, otherwise [[None]].
    */
  def asNumber: Option[BigDecimal] = None

  /**
    * Returns current json value as json null.
    *
    * @return current json value as json null if possible, otherwise [[None]].
    */
  def asNull: Option[Unit] = None

  /**
    * Returns current json value as json int.
    *
    * @return current json value as json int if possible, otherwise [[None]].
    */
  def asInt: Option[Int] = None

  /**
    * Returns current json value as json long.
    *
    * @return current json value as json long if possible, otherwise [[None]].
    */
  def asLong: Option[Long] = None

  /**
    * Returns current json value as json double.
    *
    * @return current json value as json double if possible, otherwise [[None]].
    */
  def asDouble: Option[Double] = None

  /**
    * Returns current json value as json float.
    *
    * @return current json value as json float if possible, otherwise [[None]].
    */
  def asFloat: Option[Float] = None

  /**
    * Returns a copy of current json value.
    *
    * @return current json value.
    */
  def copy(): Json

  /**
    * Size of the string representation of this element in Json.
    *
    * @return size of the element.
    */
  def size(): Int = toString.length

  override def toString: String = Json.stringify(this)

}

/**
  * Represent a json null value.
  */
case object JsNull extends Json {

  override val isNull: Boolean = true

  override val asNull: Option[Unit] = Some(())

  override val copy: Json = this

  override val size: Int = 4

}

/**
  * Represent a json boolean value.
  *
  * @param value underlying value.
  */
sealed abstract class JsBoolean(value: Boolean) extends Json {

  override val isBoolean: Boolean = true

  override val asBoolean: Option[Boolean] = Some(value)

  override val copy: Json = this

}

/**
  * Represents Json Boolean True value.
  */
case object JsTrue extends JsBoolean(true) {

  override val size: Int = 4

}

/**
  * Represents Json Boolean False value.
  */
case object JsFalse extends JsBoolean(false) {

  override val size: Int = 5

}

/**
  * Represent a json int value.
  *
  * @param value underlying value.
  */
case class JsInt(value: Int) extends Json {

  override def isInt: Boolean = true

  override def asInt: Option[Int] = Some(value)

  override def copy(): Json = this

  // Divide and conquer implementation (only comparison)
  override lazy val size: Int = {
    // Initial offset in case of negative number
    var size = 0

    // In negative case we inverse sign
    val tmp = {
      if ((value >> 31) != 0) {
        size += 1
        value * -1
      } else {
        value
      }
    }

    // Divide and conquer
    if (tmp < 100000) {
      if (tmp < 100) {
        if (tmp < 10) {
          size += 1
        } else {
          size += 2
        }
      } else {
        if (tmp < 1000) {
          size += 3
        } else {
          if (tmp < 10000) {
            size += 4
          } else {
            size += 5
          }
        }
      }
    } else {
      if (tmp < 10000000) {
        if (tmp < 1000000) {
          size += 6
        } else {
          size += 7
        }
      } else {
        if (tmp < 100000000) {
          size += 8
        } else {
          if (tmp < 1000000000) {
            size += 9
          } else {
            size += 10
          }
        }
      }
    }
    size
  }

}

/**
  * Represent a json long value.
  *
  * @param value underlying value.
  */
case class JsLong(value: Long) extends Json {

  override def isLong: Boolean = true

  override def asLong: Option[Long] = Some(value)

  override def copy(): Json = this

  // Dividing with powers of two
  override lazy val size: Int = {
    // Initial offset in case of negative number
    var size = 1

    // In negative case we inverse sign
    var tmp = {
      if ((value >> 63) != 0) {
        size += 1
        value * -1
      } else {
        value
      }
    }

    if (tmp >= 10000000000000000L) {
      size += 16
      tmp /= 10000000000000000L
    }
    if (tmp >= 100000000) {
      size += 8
      tmp /= 100000000
    }
    if (tmp >= 10000) {
      size += 4
      tmp /= 10000
    }
    if (tmp >= 100) {
      size += 2
      tmp /= 100
    }
    if (tmp >= 10) {
      size += 1
    }
    size
  }

}

/**
  * Represent a json float value.
  *
  * @param value underlying value.
  */
case class JsFloat(value: Float) extends Json {

  override def isFloat: Boolean = true

  override def asFloat: Option[Float] = Some(value)

  override def copy(): Json = this

}

/**
  * Represent a json double value.
  *
  * @param value underlying value.
  */
case class JsDouble(value: Double) extends Json {

  override def isDouble: Boolean = true

  override def asDouble: Option[Double] = Some(value)

  override def copy(): Json = this

}

/**
  * Represent a json big decimal value.
  *
  * @param value underlying value.
  */
case class JsBigDecimal(value: BigDecimal) extends Json {

  override def isNumber: Boolean = true

  override def asNumber: Option[BigDecimal] = Some(value)

  override def copy(): Json = this

}

/**
  * Represent a json string value.
  *
  * @param value underlying value.
  *
  */
case class JsString(value: String) extends Json {

  override def isString: Boolean = true

  override def asString: Option[String] = Some(value)

  override def copy(): Json = this

  override val size: Int = value.length + 2

}

/**
  * Represent a json bytes value.
  *
  * @param value underlying value.
  */
case class JsBytes(value: ByteString) extends Json {

  override def isBytes: Boolean = true

  override def asBytes: Option[ByteString] = Some(value)

  override def copy(): Json = this

}

/**
  * Represent a json array value.
  *
  * @param underlying underlying structure.
  */
case class JsArray private[json](
  private[json] val underlying: ArrayBuffer[Json]
) extends Json {

  /**
    * Get element at a given index.
    *
    * @param idx index to use.
    * @return element at given index.
    */
  def apply(idx: Int): Option[Json] = {
    if (idx >= 0 && idx < underlying.length) {
      Some(underlying(idx))
    } else {
      None
    }
  }

  /**
    * Retrieve the head element of this json array.
    *
    * @return head element of this json array.
    */
  def head(): Option[Json] = {
    if (underlying.nonEmpty) {
      Some(underlying(0))
    } else {
      None
    }
  }

  /**
    * Retrieve the last element of this json array.
    *
    * @return last element of this json array.
    */
  def last(): Option[Json] = {
    if (underlying.nonEmpty) {
      Some(underlying(underlying.length - 1))
    } else {
      None
    }
  }

  /**
    * Append an other array with the elements of this array.
    *
    * @param other array to append with.
    * @return new json array.
    */
  def append(other: JsArray): JsArray = {
    val builder = new ArrayBuffer[Json](underlying.length + other.underlying.length)
    builder ++= underlying
    builder ++= other.underlying
    JsArray(builder)
  }

  /**
    * Append an element to this array.
    *
    * @param el element to append.
    * @return new json array.
    */
  def append(el: Json): JsArray = {
    val builder = new ArrayBuffer[Json](underlying.length + 1)
    builder ++= underlying
    builder += el
    JsArray(builder)
  }

  /**
    * Prepend this array with the elements of an other array.
    *
    * @param other array to prepend with.
    * @return new json array.
    */
  def prepend(other: JsArray): JsArray = {
    val builder = new ArrayBuffer[Json](other.underlying.length + underlying.length)
    builder ++= other.underlying
    builder ++= underlying
    JsArray(builder)
  }

  /**
    * Prepend an element to this array.
    *
    * @param el element to prepend.
    * @return new json array.
    */
  def prepend(el: Json): JsArray = {
    val builder = new ArrayBuffer[Json](underlying.length + 1)
    builder += el
    builder ++= underlying
    JsArray(builder)
  }

  /**
    * Returns an Iterator over the elements in this JsArray.
    *
    * @return an Iterator containing all elements of this JsArray.
    */
  def toIterator: Iterator[Json] = underlying.toIterator

  override def isArray: Boolean = true

  override def asArray: Option[JsArray] = Some(this)

  def copy(): Json = JsArray(underlying.clone())

  override lazy val size: Int = underlying.map(_.size()).sum + 1 + underlying.size

}

/**
  * Json array builder that allow zero copy json array creation.
  *
  * @param modifiable guard modification after result.
  * @param underlying underlying data structure.
  */
case class JsArrayBuilder private(
  private var modifiable: Boolean = true,
  private val underlying: mutable.ArrayBuffer[Json] = new mutable.ArrayBuffer[Json]
) {

  /**
    * Removes the last element from this [[JsArrayBuilder]].
    *
    * @return json array builder.
    */
  def remove(): JsArrayBuilder = {
    if (modifiable) {
      underlying.remove(underlying.length - 1)
    }
    this
  }

  /**
    * Add the specified value in this [[JsArrayBuilder]].
    *
    * @param el value to add.
    * @return json array builder.
    */
  def add(el: Json): JsArrayBuilder = {
    if (modifiable) {
      underlying += el
    }
    this
  }

  /**
    * Add the specified value in this [[JsArrayBuilder]].
    *
    * @param other all values to add from other [[JsArrayBuilder]].
    * @return json array builder.
    */
  def addAll(other: JsArrayBuilder): JsArrayBuilder = {
    if (modifiable) {
      underlying ++= other.underlying
    }
    this
  }

  /**
    * Create a new [[JsArray]].
    *
    * @return new json array.
    */
  def result(): JsArray = {
    modifiable = false
    JsArray(underlying)
  }

}

/**
  * Represent a json object value.
  *
  * @param underlying underlying structure.
  */
case class JsObject private[json](
  private[json] val underlying: mutable.Map[String, Json]
) extends Json {

  /**
    * Return all fields as a seq.
    *
    * @return seq that contains all fields.
    */
  def fields: Seq[(String, Json)] = underlying.toSeq

  /**
    * Return all fields as a set.
    *
    * @return set that contains all fields.
    */
  def fieldSet: Set[(String, Json)] = underlying.toSet

  /**
    * Return all values.
    *
    * @return all values as iterable.
    */
  def values: Iterable[Json] = underlying.values

  /**
    * Returns the value to which the specified key is mapped, or JsUndefined.
    *
    * @param key key whose associated value is to be returned.
    * @return the value to which the specified key is mapped, or JsUndefined.
    */
  def apply(key: String): Option[Json] = underlying.get(key)

  /**
    * Merge this object with another one.
    *
    * @return new json object merged.
    */
  def merge(other: JsObject): JsObject = {
    val builder = mutable.AnyRefMap[String, Json]()
    builder.sizeHint(underlying.size + other.underlying.size)
    builder ++= underlying
    builder ++= other.underlying
    JsObject(builder)
  }

  /**
    * Removes the specified key from this map if present.
    *
    * @param key key whose mapping is to be removed from the json object.
    * @return new json object.
    */
  def remove(key: String): JsObject = {
    if (underlying.contains(key)) {
      val builder = mutable.AnyRefMap[String, Json]()
      builder ++= underlying
      JsObject(builder -= key)
    } else {
      this
    }
  }

  /**
    * Put the specified value with the specified key in this JsObject.
    *
    * @param field key and value to be associated.
    * @return new json object.
    */
  def put(field: (String, Json)): JsObject = {
    val builder = mutable.AnyRefMap[String, Json]()
    builder.sizeHint(underlying.size + 1)
    builder ++= underlying
    builder += field
    JsObject(builder)
  }

  override def deepMerge(other: Json): Option[JsObject] = other.asObject.map { x =>
    def merge(existingObject: JsObject, otherObject: JsObject): JsObject = {
      val result = existingObject.underlying ++ otherObject.underlying.map {
        case (otherKey, otherValue) =>
          val maybeExistingValue = existingObject.underlying.get(otherKey)

          val newValue = (maybeExistingValue, otherValue) match {
            case (Some(e: JsObject), o: JsObject) => merge(e, o)
            case _ => otherValue
          }
          otherKey -> newValue
      }
      JsObject(result)
    }

    merge(this, x)
  }

  override def isObject: Boolean = true

  override def asObject: Option[JsObject] = Some(this)

  def copy(): Json = JsObject(underlying.clone())

  override lazy val size: Int =
    1 + underlying.keys.map(_.length + 2).sum + underlying.values.map(_.size()).sum + (underlying.values.size * 2)

}

/**
  * Json object builder that allow zero copy json object creation.
  *
  * @param modifiable guard modification after result.
  * @param underlying underlying data structure.
  */
case class JsObjectBuilder private(
  private var modifiable: Boolean = true,
  private val underlying: mutable.Map[String, Json] = new mutable.AnyRefMap[String, Json]
) {

  /**
    * Returns the value to which the specified key is mapped.
    *
    * @param key the key whose associated value is to be returned.
    * @return the value to which the specified key is mapped, or [[None]] if this map contains no mapping for the key.
    */
  def get(key: String): Option[Json] = underlying.get(key)

  /**
    * Returns true if this map contains a mappping for the specified key.
    *
    * @param key key whose presence in this map is to be tested.
    * @return true if this map contains a mapping for the specified key.
    */
  @inline def contains(key: String): Boolean = underlying.contains(key)

  /**
    * Removes the specified key from this map if present.
    *
    * @param key key whose mapping is to be removed from the [[JsObjectBuilder]].
    * @return json object builder.
    */
  def remove(key: String): JsObjectBuilder = {
    if (modifiable) {
      underlying.remove(key)
    }
    this
  }

  /**
    * Put the specified value with the specified key in this [[JsObjectBuilder]].
    *
    * @param field key and value to be associated.
    * @return json object builder.
    */
  def put(field: (String, Json)): JsObjectBuilder = {
    if (modifiable) {
      underlying += field
    }
    this
  }

  /**
    * Put all values in this [[JsObjectBuilder]].
    *
    * @param other other json object builder to merge with.
    * @return json object builder.
    */
  def putAll(other: JsObjectBuilder): JsObjectBuilder = {
    if (modifiable) {
      underlying ++= other.underlying
    }
    this
  }

  /**
    * Create a new [[JsObject]].
    *
    * @return new json object.
    */
  def result(): JsObject = {
    modifiable = false
    JsObject(underlying)
  }

}
