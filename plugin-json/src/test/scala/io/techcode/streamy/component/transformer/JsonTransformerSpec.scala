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
package io.techcode.streamy.component.transformer

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.testkit.{ImplicitSender, TestKit}
import io.techcode.streamy.component.transformer.JsonTransformer.{Config, Mode}
import io.techcode.streamy.util.json._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * Json transformer spec.
  */
class JsonTransformerSpec extends TestKit(ActorSystem("JsonTransformerSpec"))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Json transformer" should {
    "be used in a flow" in {
      Source.single(Json.obj("message" -> """{"message":"foobar"}"""))
        .via(JsonTransformer.transformer(Config(Root / "message")))
        .runWith(TestSink.probe[Json])
        .requestNext() should equal(Json.obj("message" -> Json.obj("message" -> "foobar")))
    }

    "deserialize correctly a packet inplace" in {
      val input = Json.obj("message" -> """{"message":"foobar"}""")
      val component = new JsonTransformer(Config(Root / "message"))
      component.apply(input) should equal(Json.obj("message" -> Json.obj("message" -> "foobar")))
    }

    "deserialize correctly a packet with a root target" in {
      val input = Json.obj("message" -> """{"test":"foobar"}""")
      val component = new JsonTransformer(Config(Root / "message", Some(Root)))
      component.apply(input) should equal(Json.obj(
        "message" -> """{"test":"foobar"}""",
        "test" -> "foobar"
      ))
    }

    "deserialize correctly a packet with a root target equal to an existing field" in {
      val input = Json.obj("message" -> """{"message":"foobar"}""")
      val component = new JsonTransformer(Config(Root / "message", Some(Root)))
      component.apply(input) should equal(Json.obj("message" -> "foobar"))
    }

    "fast skip correctly a packet with a wrong source field" in {
      val input = Json.obj("message" -> "foobar")
      val component = new JsonTransformer(Config(Root / "message"))
      component.apply(input) should equal(Json.obj("message" -> "foobar"))
    }

    "skip correctly a packet with a wrong source field" in {
      val input = Json.obj("message" -> "{foobar}")
      val component = new JsonTransformer(Config(Root / "message"))
      component.apply(input) should equal(Json.obj("message" -> "{foobar}"))
    }

    "serialize correctly a packet inplace" in {
      val input = Json.obj("message" -> Json.obj("message" -> "foobar"))
      val component = new JsonTransformer(Config(Root / "message", mode = Mode.Serialize))
      component.apply(input) should equal(Json.obj("message" -> """{"message":"foobar"}"""))
    }

    "serialize correctly a packet with a root source" in {
      val input = Json.obj("test" -> "foobar")
      val component = new JsonTransformer(Config(Root, Some(Root / "message"), mode = Mode.Serialize))
      component.apply(input) should equal(Json.obj(
        "message" -> """{"test":"foobar"}""",
        "test" -> "foobar"
      ))
    }

    "serialize correctly a packet with a root target equal to an existing field" in {
      val input = Json.obj("test" -> "foobar")
      val component = new JsonTransformer(Config(Root, Some(Root / "test"), mode = Mode.Serialize))
      component.apply(input) should equal(Json.obj(
        "test" -> """{"test":"foobar"}"""
      ))
    }
  }

}