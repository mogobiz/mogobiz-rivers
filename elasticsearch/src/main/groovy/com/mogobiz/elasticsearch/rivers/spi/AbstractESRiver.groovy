/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.elasticsearch.rivers.spi

import com.mogobiz.common.client.BulkItem
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.client.Client
import com.mogobiz.common.client.Item
import com.mogobiz.common.rivers.spi.AbstractRiver
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.elasticsearch.client.ESClient
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 *
 */
abstract class AbstractESRiver<E> extends AbstractRiver<E> implements ESRiver{

    private final Client client = ESClient.instance

    protected AbstractESRiver(){}

    @Override
    final Future<BulkResponse> bulk(RiverConfig config, List<BulkItem> items, ExecutionContext ec) {
        return client.bulk(config, items, ec)
    }

    @Override
    abstract Item asItem(E e, RiverConfig config)

    @Override
    String getUuid(E e) {
        super.getUuid(e)
    }

}
