package com.mogobiz.common.client

import com.mogobiz.common.rivers.spi.RiverConfig
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 * Created by smanciot on 16/05/2014.
 */
interface Client {

    rx.Observable<Future<BulkResponse>> bulk(
            final RiverConfig config,
            final List<BulkItem> items,
            ExecutionContext ec)


    SearchResponse search(Request request)

}
