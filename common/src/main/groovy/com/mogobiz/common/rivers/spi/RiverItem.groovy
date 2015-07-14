package com.mogobiz.common.rivers.spi

import com.mogobiz.common.client.BulkItem

/**
 *
 * Created by smanciot on 10/03/15.
 */
interface RiverItem {

	String getKey()

    BulkItem asBulkItem(RiverConfig config)
}