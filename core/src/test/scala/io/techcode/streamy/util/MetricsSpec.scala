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
package io.techcode.streamy.util

import akka.actor.ActorSystem
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import org.slf4j.Logger
import org.mockito.Mockito._

/**
  * Metrics spec.
  */
class MetricsSpec extends FlatSpec with MockitoSugar with OneInstancePerTest with Matchers {

  // Name of application
  val ApplicationName = "streamy"

  // Actor system
  implicit val system: ActorSystem = ActorSystem(ApplicationName)

  // Logger
  val loggerMock: Logger = mock[Logger]

  "Metrics" must "logs some informations" in {
    Metrics.reporter(system, loggerMock, system.settings.config)
    verify(loggerMock, times(1))
  }

}