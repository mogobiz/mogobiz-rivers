/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.common.client

import com.mogobiz.common.rivers.spi.RiverConfig
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 * Created by smanciot on 16/05/2014.
 */
interface Client {

    Future<BulkResponse> bulk(
            final RiverConfig config,
            final List<BulkItem> items,
            ExecutionContext ec)


    SearchResponse search(Request request)

}
