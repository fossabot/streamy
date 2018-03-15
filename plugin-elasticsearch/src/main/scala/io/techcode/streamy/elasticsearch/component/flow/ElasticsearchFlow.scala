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
package io.techcode.streamy.elasticsearch.component.flow

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Merge, Source}
import akka.stream.stage._
import akka.util.ByteString
import com.softwaremill.sttp._
import io.techcode.streamy.elasticsearch.event.{ElasticsearchDropEvent, ElasticsearchFailureEvent, ElasticsearchPartialEvent, ElasticsearchSuccessEvent}
import io.techcode.streamy.util.json._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Elasticsearch flow companion.
  */
object ElasticsearchFlow {

  // Default values
  val DefaultBulk: Int = 500
  val DefaultWorker: Int = 1
  val DefaultRetry: FiniteDuration = 1 second

  // Component configuration
  case class Config(
    hosts: Seq[String],
    indexName: String,
    typeName: String,
    action: String,
    bulk: Int = DefaultBulk,
    worker: Int = DefaultWorker,
    retry: FiniteDuration = DefaultRetry
  )

  /**
    * Create a new elasticsearch flow.
    *
    * @param config sink configuration.
    * @return sink.
    */
  def apply(config: Config)(
    implicit httpClient: SttpBackend[Future, Source[ByteString, NotUsed]],
    system: ActorSystem,
    executionContext: ExecutionContext
  ): Flow[Json, Json, NotUsed] = {
    if (config.worker > 1) {
      balancer(Flow.fromGraph(new ElasticsearchFlowStage(config)), config.worker)
    } else {
      Flow.fromGraph(new ElasticsearchFlowStage(config))
    }
  }

  /**
    * Balancing jobs to a fixed pool of workers.
    *
    * @param worker      worker logic.
    * @param workerCount worker count.
    * @tparam In  input type.
    * @tparam Out output type.
    * @return balanced flow.
    */
  private def balancer[In, Out](worker: Flow[In, Out, Any], workerCount: Int): Flow[In, Out, NotUsed] = {
    import akka.stream.scaladsl.GraphDSL.Implicits._

    Flow.fromGraph(GraphDSL.create() { implicit b ⇒
      val balancer = b.add(Balance[In](workerCount, waitForAllDownstreams = true))
      val merge = b.add(Merge[Out](workerCount))

      for (_ ← 1 to workerCount) {
        // for each worker, add an edge from the balancer to the worker, then wire
        // it to the merge element
        balancer ~> worker.async ~> merge
      }

      FlowShape(balancer.in, merge.out)
    })
  }

  /**
    * Elasticsearch flow stage.
    *
    * @param config flow stage configuration.
    */
  private class ElasticsearchFlowStage(config: Config)(
    implicit val httpClient: SttpBackend[Future, Source[ByteString, NotUsed]],
    system: ActorSystem,
    executionContext: ExecutionContext
  ) extends GraphStage[FlowShape[Json, Json]] {

    // Inlet
    val in: Inlet[Json] = Inlet("ElasticsearchFlow.in")

    // Outlet
    val out: Outlet[Json] = Outlet("ElasticsearchFlow.out")

    // Shape
    override val shape: FlowShape[Json, Json] = FlowShape.of(in, out)

    // Logic generator
    override def createLogic(attr: Attributes): TimerGraphStageLogic = new ElasticsearchFlowLogic

    /**
      * Marshal all packets.
      *
      * @param pkts packets to process.
      * @return prepared request in bulk format.
      */
    private def marshalMessages(pkts: Seq[Json]): Array[Byte] = {
      pkts.map(marshalMessage)
        .reduce((x, y) => x ++ y)
        .compact.toArray[Byte]
    }

    /**
      * Marshal a single packet.
      *
      * @param pkt packet to marshal.
      * @return bytestring representation.
      */
    private def marshalMessage(pkt: Json): ByteString = {
      // Retrive header information
      val id = pkt.evaluate(Root / "_id").asString
      val `type` = pkt.evaluate(Root / "_type").asString.getOrElse(config.typeName)
      val version = pkt.evaluate(Root / "_version").asLong
      val versionType = pkt.evaluate(Root / "_version_type").asString

      // Build header
      val header = Json.obj(config.action -> {
        val builder = Json.objectBuilder()
          .put("_index" -> config.indexName)
          .put("_type" -> `type`)

        // Add version if present
        if (version.nonEmpty) {
          builder.put("_version" -> version.get)
        }

        // Add version type if present
        if (versionType.nonEmpty) {
          builder.put("_version_type" -> versionType.get)
        }

        // Add id if present
        if (id.nonEmpty) {
          builder.put("_id" -> id.get)
        }
        builder.result()
      })

      // Remove extra fields
      val doc = pkt.patch(Bulk(Root, Seq(
        Remove(Root / "_id", mustExist = false),
        Remove(Root / "_type", mustExist = false),
        Remove(Root / "_version", mustExist = false),
        Remove(Root / "_version_type", mustExist = false)
      ))).get
      ByteString(header.toString()) ++ ByteString("\n") ++ ByteString(doc.toString()) ++ ByteString("\n")
    }

    // Handle response as json
    private val asJson: ResponseAs[Json, Nothing] = asByteArray.map(Json.parse(_).getOrElse(JsNull))

    /**
      * Elasticsearch flow logic.
      */
    private class ElasticsearchFlowLogic extends TimerGraphStageLogic(shape)
      with InHandler with OutHandler with StageLogging {

      // Set handler
      setHandlers(in, out, this)

      // Pending message
      private val buffer = new mutable.Queue[Json]

      // Async success handler
      private val successHandler = getAsyncCallback[Response[Json]](handleResponse)

      // Async failure handler
      private val failureHandler = getAsyncCallback[Throwable](handleFailure)

      // State of the logic
      private var state = State.Idle

      // Start request time
      private var started: Long = System.currentTimeMillis()

      // State
      object State extends Enumeration {
        val Idle, Busy = Value
      }

      // List of hosts to use
      private val hosts: Iterator[String] = Stream.continually(config.hosts.toStream).flatten.toIterator

      // Current processing message
      private var messages: Seq[Json] = Nil

      /**
        * Handle response success.
        *
        * @param response http response.
        */
      def handleResponse(response: Response[Json]): Unit = {
        response.body match {
          case Left(ex) =>
            log.error(ex)
            handleFailure(NotUsed)
          case Right(data) =>
            val errors = data.evaluate(Root / "errors").asBoolean
            if (errors.getOrElse(true)) {
              processPartial(data)
            } else {
              processSuccess()
            }
        }
      }

      /**
        * Handle request failure.
        *
        * @param ex request exception.
        */
      @inline def handleFailure(ex: Any): Unit = processFailure()

      /**
        * Process success elements.
        */
      def processSuccess(): Unit = {
        system.eventStream.publish(ElasticsearchSuccessEvent(elapsed()))
        val results = messages
        messages = Nil
        state = State.Idle
        emitMultiple(out, results.toIterator, () => performRequest())
      }

      /**
        * Process partial elements.
        */
      def processPartial(data: Json): Unit = {
        // Handle failed items
        val items = data.evaluate(Root / "items").asArray.get

        var backPressure = false
        val results = messages.zip(items.toSeq)
          .map(x => (x._1.asObject.get, x._2.asObject.get))
          .filter { case (item, result) =>
            val status = result.evaluate(Root / config.action / "status").asInt.get

            // We can't do anything in case of conflict or bad request or not found
            if (status == 409 || status == 400 || status == 404) {
              system.eventStream.publish(ElasticsearchDropEvent(item, result))
              false
            } else {
              if (status == 429) backPressure = true
              true
            }
          }
          .groupBy { case (_, result) =>
            val status = result.evaluate(Root / config.action / "status").asInt.get
            if (status < 300) 0 else 1
          }

        messages = Nil
        results.getOrElse(1, Nil).map(_._1).foreach(buffer.enqueue(_))

        if (backPressure) {
          scheduleOnce(NotUsed, config.retry)
        } else {
          state = State.Idle
        }
        system.eventStream.publish(ElasticsearchPartialEvent(elapsed()))
        emitMultiple(out, results.getOrElse(0, Nil).map(_._1).toIterator, () => performRequest())
      }

      /**
        * Process failure elements.
        */
      def processFailure(): Unit = {
        system.eventStream.publish(ElasticsearchFailureEvent(elapsed()))
        messages.foreach(buffer.enqueue(_))
        messages = Nil
        scheduleOnce(NotUsed, config.retry)
      }

      /**
        * Try to pull an element in the buffer.
        */
      private def tryPull(): Unit = {
        if (buffer.lengthCompare(config.bulk) < 0 && !isClosed(in) && !hasBeenPulled(in)) {
          pull(in)
        }
      }

      /**
        * Prepare message to send in request.
        */
      def prepareElems(): Unit = {
        messages = (1 to config.bulk).flatMap { _ =>
          buffer.dequeueFirst(_ => true)
        }
      }

      /**
        * Perform a request if idle.
        */
      def performRequest(): Unit = {
        if (isClosed(in) && buffer.isEmpty) {
          completeStage()
        } else if (state == State.Idle) {
          prepareElems()

          // Perform request
          if (messages.nonEmpty) {
            state = State.Busy
            started = System.currentTimeMillis()
            sttp
              .post(uri"${hosts.next()}/_bulk")
              .header("Content-Type", "application/x-ndjson")
              .body(marshalMessages(messages))
              .response(asJson)
              .readTimeout(5 seconds)
              .send()
              .onComplete {
                case Success(response) => successHandler.invoke(response)
                case Failure(ex) => failureHandler.invoke(ex)
              }
          }
        }
      }

      /**
        * Gets time elapsed between begin of request and now.
        */
      def elapsed(): Long = System.currentTimeMillis() - started

      // Start pulling on materialize
      override def preStart(): Unit = pull(in)

      // We have schedule a retry or backpressure
      override def onTimer(timerKey: Any): Unit = {
        state = State.Idle
        performRequest()
      }

      // On demand try to pull
      override def onPull(): Unit = tryPull()

      override def onPush(): Unit = {
        // Keep going, we handle stage complete after last response
        setKeepGoing(true)

        // Get element
        buffer.enqueue(grab(in))

        // Perform request if idle
        performRequest()

        // Don't forget to fill buffer
        tryPull()
      }

      override def onUpstreamFinish(): Unit = {
        // Base on state
        state match {
          case State.Idle => completeStage()
          case State.Busy => ()
        }
      }

    }

  }

}
