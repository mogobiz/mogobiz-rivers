/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz.mirakl.rivers

import akka.stream.scaladsl._
import com.mogobiz.common.rivers.spi.RiverConfig
import org.reactivestreams.{Publisher, Subscriber}
import rx.Observable
import rx.functions.Func1
import rx.internal.reactivestreams.PublisherAdapter

import scala.concurrent.{Future, ExecutionContext}

/**
+----------+
|          |
|  Source  |  Entity
|          |
+----------+
     |
     v
+----------+
|          |
|    map   |  In
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
object MiraklRiverFlow {

  def synchronize[Entity, In, Out](river: MiraklRiver[Entity, In, Out], config: RiverConfig, balanceSize: Int = 2, bulkSize: Int = 100, subscriber: Subscriber[Out]): Unit = {
    import com.mogobiz.common.rivers._
    import RiversFlow._

    subscribe({
      Source() { implicit b =>
        import FlowGraphImplicits._

        val source: Source[In] = Source(river.publisher(config))
        val group = Flow[In].grouped(bulkSize)

        import scala.collection.JavaConversions._

        val bulk = Flow[Seq[In]].mapAsyncUnordered[Out](items => river.bulk(config, items.toList, system.dispatcher))

        val undefinedSink = UndefinedSink[Out]

        if(balanceSize > 1){
          val balance = Balance[Seq[In]]
          val merge = Merge[Out]

          source ~> group ~> balance

          1 to balanceSize foreach{ _ =>
            balance ~> bulk ~> merge
          }

          merge ~> undefinedSink
        }
        else{
          source ~> group ~> bulk ~> undefinedSink
        }

        undefinedSink
      }
    })(Seq(subscriber)).run()

  }

}

trait MiraklRiver[Entity, In, Out] {
  def exportCatalogItemsAsRiverItems(config: RiverConfig): Observable[In] = {
    retrieveCatalogItems(config).flatMap(new Func1[Entity, Observable[In]](){
      override def call(e: Entity): Observable[In] = Observable.just(asRiverItem(e, config))
    })
  }

  def retrieveCatalogItems(config: RiverConfig): Observable[Entity]

  def asRiverItem(e:Entity, config: RiverConfig): In

  import java.util

  def bulk(config: RiverConfig, items: util.List[In], ec: ExecutionContext): Future[Out]

  def publisher(config: RiverConfig): Publisher[In] = {
    new PublisherAdapter[In](exportCatalogItemsAsRiverItems(config))
  }
}

abstract class AbstractMiraklRiver[Entity, In, Out] extends MiraklRiver[Entity, In, Out]
