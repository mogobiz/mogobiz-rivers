/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.elasticsearch.rivers

import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.rivers.GenericRiversFlow
import com.mogobiz.common.rivers.spi.RiverConfig

import org.reactivestreams.Subscriber

/**
 *
 */
object ESRiversFlow {

  def exportRiversItemsWithSubscription[T <: ESRivers](rivers: T, config: RiverConfig, balanceSize: Int = 2, bulkSize: Int = 100, subscriber: Subscriber[BulkResponse]): Unit = {

    import GenericRiversFlow._

    publish(rivers, config,balanceSize, bulkSize, subscriber)
  }

}
