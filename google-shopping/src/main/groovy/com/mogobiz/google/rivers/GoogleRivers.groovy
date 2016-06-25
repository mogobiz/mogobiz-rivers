/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.google.rivers

import com.mogobiz.common.client.BulkItem
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.client.Client
import com.mogobiz.common.rivers.AbstractGenericRivers
import com.mogobiz.common.rivers.spi.GenericRiver
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.google.client.GoogleClient
import com.mogobiz.google.rivers.spi.AbstractGoogleRiver
import com.mogobiz.google.rivers.spi.GoogleRiver
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 */
class GoogleRivers extends AbstractGenericRivers<BulkItem, BulkResponse> {

    static GoogleRivers instance

    static final Client client = GoogleClient.instance

    private GoogleRivers(){}

    static GoogleRivers getInstance(){
        if(!instance){
            instance = new GoogleRivers()
        }
        instance
    }

    @Override
    Class<GoogleRiver> river() {
        return GoogleRiver.class
    }

    @Override
    Future<BulkResponse> bulk(RiverConfig config, List<BulkItem> items, ExecutionContext ec) {
        return client.bulk(config, items, ec)
    }
}
