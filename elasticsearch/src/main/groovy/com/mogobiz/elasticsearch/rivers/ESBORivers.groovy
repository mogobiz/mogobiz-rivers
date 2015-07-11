package com.mogobiz.elasticsearch.rivers

import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.BulkItem
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.rivers.AbstractRiverCache
import com.mogobiz.common.rivers.Rivers
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.common.rivers.spi.RiverItem
import com.mogobiz.elasticsearch.client.*
import com.mogobiz.elasticsearch.rivers.spi.ESBORiver
import org.reactivestreams.Publisher
import rx.Observable
import rx.functions.Action1
import rx.functions.Func0
import rx.functions.Func1
import rx.internal.reactivestreams.ObservableToPublisherAdapter
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 *
 * Created by stephane.manciot@ebiznext.com on 11/07/2015.
 */
final class ESBORivers extends AbstractESRivers<ESBORiver> {

    static ESBORivers instance

    /**
     *
     */
    private ESBORivers() { super(ESBORiver.class) }

    static ESBORivers getInstance() {
        if (instance == null) {
            instance = new ESBORivers()
        }
        instance
    }

    @Override
    protected ESIndexResponse createCompanyIndex(RiverConfig config) {
        ESIndexResponse response = new ESIndexResponse(acknowledged: true, error: null)
        def mappings = [:]
        loadRivers().each { river ->
            mappings.putAll(river.defineESMappingAsMap())
        }
        def index = config.clientConfig.store ?: "mogopay"
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
    protected Collection<Observable<Future<BulkResponse>>> iterable(RiverConfig config, ExecutionContext ec) {
        Collection<Observable<Future<BulkResponse>>> iterable = []
        loadRivers().each { river ->
            iterable << river.exportCatalogItems(config, ec, 100)
        }
        iterable
    }
}
