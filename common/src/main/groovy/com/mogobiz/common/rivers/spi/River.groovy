/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.common.rivers.spi

import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.client.SearchResponse
import org.reactivestreams.Publisher
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 * Created by stephane.manciot@ebiznext.com on 14/05/2014.
 */
public interface River {

    Publisher<RiverItem> exportCatalogItemsAsPublisher(final RiverConfig config)

    rx.Observable<RiverItem> exportCatalogItemsAsRiverItems(final RiverConfig config)

    rx.Observable<Future<BulkResponse>> exportCatalogItems(
            final RiverConfig config,
            final ExecutionContext ec,
            final int count)

    SearchResponse retrieveCatalogPreviousItems(final RiverConfig config)

    String getType()
}