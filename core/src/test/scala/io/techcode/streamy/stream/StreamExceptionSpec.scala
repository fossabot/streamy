/*
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2017
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
package io.techcode.streamy.stream

import org.scalatest._
import play.api.libs.json.Json

/**
  * Stream exception spec.
  */
class StreamExceptionSpec extends FlatSpec with Matchers {

  "StreamException" must "not have a stacktrace" in {
    new StreamException("foobar").getStackTrace.length should equal(0)
  }

  it should "be convert to json without packet" in {
    new StreamException("foobar").toJson should equal(Json.obj("message" -> "foobar", "packet" -> Json.obj()))
  }

  it should "be concert to json with packet" in {
    new StreamException("foobar", Some(Json.obj("details" -> "test"))).toJson should equal(Json.obj(
      "message" -> "foobar", "packet" -> Json.obj("details" -> "test")
    ))
  }

}