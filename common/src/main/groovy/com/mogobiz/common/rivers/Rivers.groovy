/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz.common.rivers

import com.mogobiz.common.client.BulkItem
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.rivers.spi.River

/**
 *
 */
interface Rivers<T extends River> extends GenericRivers<BulkItem, BulkResponse>{

}
