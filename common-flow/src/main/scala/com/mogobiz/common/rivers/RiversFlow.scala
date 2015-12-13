/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.common.rivers

import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._

import com.mogobiz.common.client.{BulkResponse, BulkItem, Client}
import com.mogobiz.common.rivers.spi.{RiverConfig, RiverItem}

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

/**
 *
 */
object RiversFlow extends BootedRiversSystem {

  implicit val flowMaterializer = ActorFlowMaterializer()

  def exportRiversItemsWithSubscription(publisher: Publisher[RiverItem], config: RiverConfig, client: Client, balanceSize:Int = 2, bulkSize: Int = 100, subscriber: Subscriber[BulkResponse]): Unit = {
    subscribe(exportRiversItems(publisher, config, client, balanceSize, bulkSize))(Seq(subscriber)).run()
  }

  /**

   +----------+
   |          |
   |  Source  |
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
   |   Sink   |
   |          |
   +----------+

  */
  def exportRiversItems(publisher: Publisher[RiverItem], config: RiverConfig, client: Client, balanceSize:Int = 2, bulkSize: Int = 100): Source[BulkResponse] = Source() { implicit b =>
    import FlowGraphImplicits._

    val source: Source[RiverItem] = Source(publisher)
    val map = Flow[RiverItem].map[BulkItem](_.asBulkItem(config))
    val group = Flow[BulkItem].grouped(bulkSize)

    import scala.collection.JavaConversions._

    val bulk = Flow[Seq[BulkItem]].mapAsyncUnordered[BulkResponse](items => client.bulk(config, items.toList, system.dispatcher))

    val undefinedSink = UndefinedSink[BulkResponse]

    if(balanceSize > 1){
      val balance = Balance[Seq[BulkItem]]
      val merge = Merge[BulkResponse]

      source ~> map ~> group ~> balance

      1 to balanceSize foreach{ _ =>
        balance ~> bulk ~> merge
      }

      merge ~> undefinedSink
    }
    else{
      source ~> map ~> group ~> bulk ~> undefinedSink
    }

    undefinedSink
  }

}
