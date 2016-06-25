/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.elasticsearch.rivers

import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.elasticsearch.client.*
import com.mogobiz.elasticsearch.rivers.spi.ESBORiver
import rx.Observable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 *
 */
final class ESBORivers extends AbstractESRivers<ESBORiver> {

    static ESBORivers instance

    /**
     *
     */
    private ESBORivers() {  }

    static ESBORivers getInstance() {
        if (instance == null) {
            instance = new ESBORivers()
        }
        instance
    }

    @Override
    ESIndexResponse createCompanyIndex(RiverConfig config) {
        ESIndexResponse response = new ESIndexResponse(acknowledged: true, error: null)
        def mappings = [:]
        loadRivers().each { ESBORiver river ->
            mappings.putAll(river.defineESMappingAsMap())
        }
        def index = config.clientConfig.config.index as String
        def exists = client.indexExists(config.clientConfig.url, index)
        if (!exists) {
            response = client.createIndex(
                    config.clientConfig.url,
                    index,
                    new ESIndexSettings(
                            number_of_replicas: config.clientConfig.config.replicas as Integer ?: 1,
                            refresh_interval: "1s"
                    ),
                    mappings,
                    [debug: config.debug])
        }
        response
    }

    @Override
    protected Collection<Observable<Future<BulkResponse>>> iterable(RiverConfig config, int bulkSize = 100, ExecutionContext ec) {
        Collection<Observable<Future<BulkResponse>>> iterable = []
        loadRivers().each { ESBORiver river ->
            iterable << river.exportCatalogItems(config, ec, bulkSize)
        }
        iterable
    }

    @Override
    Class<ESBORiver> river() {
        ESBORiver.class
    }

}
