/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz.common.rivers

import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._
import com.mogobiz.common.rivers.spi.{GenericRiver, RiverConfig}
import org.reactivestreams.Subscriber

import scala.concurrent.{Future, ExecutionContext}

import scala.collection.JavaConversions._

/**
+----------+
|          |
|  Source  |  In
|          |
+----------+
     |
     v
+----------+
|          |
|    map   |
|          |
+----------+
     |
     v
+----------+
|          |
|  group   |
|          |
+----------+
     |
     v
+----------+        +----------+
|          |------->|          |
|  balance |        |   bulk   |
|          |------->|          |
+----------+        +----------+
                       |    |
                       |    |
                       |    |
+---------+            |    |
|         |<-----------'    |
|  merge  |                 |
|         |<----------------'
+---------+
     |
     v
+----------+
|          |
|   Sink   | Out
|          |
+----------+
  */
object GenericRiversFlow {

  def publish[In, Out](rivers: GenericRivers[In, Out] with BootedRiversSystem, config: RiverConfig, balanceSize: Int = 2, subscriber: Subscriber[Out]): Unit = {
    implicit val system = rivers.system
    implicit val flowMaterializer = ActorFlowMaterializer()
    subscribe({
      Source() { implicit b =>
        import FlowGraphImplicits._

        val source: Source[In] = Source(rivers.publisher(config))
        val group = Flow[In].grouped(config.bulkSize)

        val bulk = Flow[Seq[In]].mapAsyncUnordered[Out](items => rivers.bulk(config, seqAsJavaList(items.toList), rivers.dispatcher))

        val undefinedSink = UndefinedSink[Out]

        if (balanceSize > 1) {
          val balance = Balance[Seq[In]]
          val merge = Merge[Out]

          source ~> group ~> balance

          1 to balanceSize foreach { _ =>
            balance ~> bulk ~> merge
          }

          merge ~> undefinedSink
        }
        else {
          source ~> group ~> bulk ~> undefinedSink
        }

        undefinedSink
      }
    })(Seq(subscriber)).run()

  }

  def synchronize[In, Out](gr: GenericRiver[In, Out], config: RiverConfig, balanceSize: Int = 2, subscriber: Subscriber[Out]): Unit = {
    val rivers = new GenericRivers[In, Out] {
      import java.util

      override def bulk(config: RiverConfig, items: util.List[In], ec: ExecutionContext): Future[Out] = gr.bulk(config, items, ec)

      override def loadRivers: util.List[GenericRiver[In, Out]] = List(gr)
    }
    publish(rivers, config, balanceSize, subscriber)
  }
}
