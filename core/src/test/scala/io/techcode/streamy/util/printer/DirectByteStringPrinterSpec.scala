/*
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2018
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

import akka.util.ByteString
import io.techcode.streamy.util.json.Json
import org.scalatest._

/**
  * Direct bytestring printer spec.
  */
class DirectByteStringPrinterSpec extends WordSpecLike with Matchers {

  "Direct byteString printer" should {
    "print correctly a json value when success" in {
      val printer = new DirectByteStringPrinterImpl(Json.obj("foo" -> "bar"))
      printer.print() should equal(Some(ByteString("""{"foo":"bar"}""")))
    }

    "print correctly a json value when failed" in {
      val printer = new DirectByteStringPrinterImpl(Json.obj("foo" -> "bar"), false)
      printer.print() should equal(None)
    }

    "implement an error message by default" in {
      val printer = new DirectByteStringPrinterImpl(Json.obj("foo" -> "bar"))
      printer.error() should equal("Unexpected printing error occured")
    }
  }

}

class DirectByteStringPrinterImpl(pkt: Json, success: Boolean = true) extends DirectByteStringPrinter(pkt) {
  override def process(): Boolean = {
    builder.putBytes(pkt.toString.getBytes())
    success
  }
}
