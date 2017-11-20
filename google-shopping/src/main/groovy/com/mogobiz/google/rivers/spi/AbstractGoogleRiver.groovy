/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.google.rivers.spi

import com.mogobiz.common.client.BulkItem
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.client.Client
import com.mogobiz.common.client.Item
import com.mogobiz.common.rivers.spi.AbstractRiver
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.google.client.GoogleClient
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 */
abstract class AbstractGoogleRiver<E> extends AbstractRiver<E> implements GoogleRiver{

    private Client client = GoogleClient.instance

    @Override
    Future<BulkResponse> bulk(RiverConfig config, List<BulkItem> items, ExecutionContext ec) {
        return client.bulk(config, items, ec)
    }

    @Override
    abstract Item asItem(E e, RiverConfig config)
}
