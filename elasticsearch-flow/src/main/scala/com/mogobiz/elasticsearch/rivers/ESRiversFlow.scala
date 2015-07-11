package com.mogobiz.elasticsearch.rivers

import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.elasticsearch.rivers.spi.ESRiver
import com.mogobiz.elasticsearch.client.ESClient

import org.reactivestreams.Subscriber

/**
 *
 * Created by smanciot on 10/03/15.
 */
object ESRiversFlow {

  def exportRiversItemsWithSubscription[T <: ESRiver](rivers: AbstractESRivers[T], config: RiverConfig, balanceSize: Int = 2, bulkSize: Int = 100, subscriber: Subscriber[BulkResponse]): Unit = {

    import com.mogobiz.common.rivers._
    import RiversFlow._

    subscribe(exportRiversItems(rivers.publisher(config), config, ESClient.getInstance(), balanceSize, bulkSize))(Seq(subscriber)).run()
  }

}
