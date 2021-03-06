/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.elasticsearch.rivers

import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.BulkItem
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.rivers.AbstractRiverCache
import com.mogobiz.common.rivers.AbstractRivers
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.common.rivers.spi.RiverItem
import com.mogobiz.elasticsearch.client.ESClient
import com.mogobiz.elasticsearch.client.ESIndexResponse
import com.mogobiz.elasticsearch.rivers.spi.ESRiver
import org.reactivestreams.Publisher
import rx.Observable
import rx.functions.Func0
import rx.functions.Func1
import rx.internal.reactivestreams.PublisherAdapter
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 *
 */
abstract class AbstractESRivers<T extends ESRiver> extends AbstractRivers<T> {

    static final ESClient client = ESClient.instance

    /**
     *
     */
    protected AbstractESRivers() {}

    abstract ESIndexResponse createCompanyIndex(RiverConfig config)

    protected abstract Collection<Observable<Future<BulkResponse>>> iterable(RiverConfig config, ExecutionContext ec)

    @Override
    final Future<Iterator<BulkResponse>> export(RiverConfig config) {

        AbstractRiverCache.purgeAll()

        ESIndexResponse response = createCompanyIndex(config)

        if (response.acknowledged) {
            super.export(config)
        } else {
            throw new Exception("an error occured while creating index ${response.error}");
        }

    }
    @Override
    final Publisher<BulkItem> publisher(final RiverConfig config) {
        final RiverItem i18n = new RiverItem() {

            @Override
            String getKey() {
                return "i18n::1"
            }

            @Override
            BulkItem asBulkItem(RiverConfig c) {
                new BulkItem(
                        type: 'i18n',
                        action: BulkAction.UPDATE,
                        id: 1L,
                        parent: null,
                        map: ['languages': c.languages]
                )
            }
        }
        new PublisherAdapter<BulkItem>(
                Observable.defer(
                        {
                            Observable.merge(
                                    loadRivers().collect { river ->
                                        river.exportCatalogItemsAsRiverItems(config)
                                    }
                            ).distinct(
                                    { BulkItem item -> item.type + "_" + item.id } as Func1<BulkItem, String>
                            ).startWith(i18n.asBulkItem(config))
                        } as Func0<Observable<BulkItem>>
                )
        )
    }

    @Override
    Future<BulkResponse> bulk(RiverConfig config, List<BulkItem> items, ExecutionContext ec) {
        return client.bulk(config, items, ec)
    }
}
