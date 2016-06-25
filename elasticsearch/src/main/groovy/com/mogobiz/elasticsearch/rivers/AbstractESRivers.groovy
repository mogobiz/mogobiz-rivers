/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.elasticsearch.rivers

import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.BulkItem
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.rivers.AbstractGenericRivers
import com.mogobiz.common.rivers.AbstractRiverCache
import com.mogobiz.common.rivers.Rivers
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.common.rivers.spi.RiverItem
import com.mogobiz.elasticsearch.client.ESClient
import com.mogobiz.elasticsearch.client.ESIndexResponse
import com.mogobiz.elasticsearch.rivers.spi.AbstractESRiver
import com.mogobiz.elasticsearch.rivers.spi.ESRiver
import org.reactivestreams.Publisher
import rx.Observable
import rx.functions.Action1
import rx.functions.Func0
import rx.functions.Func1
import rx.internal.reactivestreams.PublisherAdapter
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 *
 */
abstract class AbstractESRivers<T extends ESRiver> extends AbstractGenericRivers<BulkItem, BulkResponse> implements Rivers<T> {

    static final ESClient client = ESClient.instance

    /**
     *
     */
    protected AbstractESRivers() {}

    abstract ESIndexResponse createCompanyIndex(RiverConfig config)


    protected abstract Collection<Observable<Future<BulkResponse>>> iterable(RiverConfig config, int bulkSize, ExecutionContext ec)

    @Override
    final Future<Iterator<BulkResponse>> export(
            RiverConfig config) {

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
                                    loadRivers().collect { AbstractESRiver river ->
                                        river.exportCatalogItemsAsRiverItems(config)
                                    }
                            ).distinct(
                                    { BulkItem item -> item.id } as Func1<BulkItem, String>
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
