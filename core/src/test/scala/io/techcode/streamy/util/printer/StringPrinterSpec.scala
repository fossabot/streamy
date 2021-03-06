/*
 * The MIT License (MIT)
 * <p>
 * Copyright (C) 2017-2019
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
package io.techcode.streamy.util.printer

import io.techcode.streamy.util.json.Json
import io.techcode.streamy.util.parser.ParseException
import org.scalatest._

/**
  * String printer spec.
  */
class StringPrinterSpec extends WordSpecLike with Matchers {

  "String printer" should {
    "print correctly a json value when success" in {
      val printer = new StringPrinterImpl()
      printer.print(Json.obj("foo" -> "bar")).toOption should equal(Some("""{"foo":"bar"}"""))
    }

    "print correctly a json value when failed" in {
      val printer = new StringPrinterImpl(false)
      printer.print(Json.obj("foo" -> "bar")).isLeft should equal(true)
    }

    "implement an error message by default" in {
      val printer = new StringPrinterImpl(false)
      printer.print(Json.obj("foo" -> "bar")).isLeft should equal(true)
    }
  }

}

class StringPrinterImpl(success: Boolean = true) extends StringPrinter[Json] {
  override def run(): String = {
    if (!success) {
      throw new PrintException("Unexpected printing error occured")
    }
    builder.append(data)
    builder.toString
  }
}
