/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.elasticsearch.rivers

import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.BulkItem
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.rivers.AbstractRiverCache
import com.mogobiz.common.rivers.Rivers
import com.mogobiz.common.rivers.spi.River
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.common.rivers.spi.RiverItem
import com.mogobiz.elasticsearch.client.ESClient
import com.mogobiz.elasticsearch.client.ESIndexResponse
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
 * Created by smanciot on 11/07/15.
 */
abstract class AbstractESRivers<R extends River> extends Rivers<R>{

    static final ESClient client = ESClient.instance

    /**
     *
     */
    protected AbstractESRivers(Class<R> riverType) {
        super(riverType)
    }

    abstract ESIndexResponse createCompanyIndex(RiverConfig config)

    protected abstract Collection<Observable<Future<BulkResponse>>> iterable(RiverConfig config, ExecutionContext ec)

    @Override
    final Future<Collection<BulkResponse>> export(
            RiverConfig config,
            ExecutionContext ec) {

        AbstractRiverCache.purgeAll()

        ESIndexResponse response = createCompanyIndex(config)

        if (response.acknowledged) {
            Collection<Future<BulkResponse>> collection = []

            Observable.merge(iterable(config, ec)).subscribe({
                collection << (it as Future<BulkResponse>)
            } as Action1<Future<BulkResponse>>,
                    { th -> th.printStackTrace(System.err) } as Action1<Throwable>)

            collect(collection, ec)
        } else {
            throw new Exception("an error occured while creating index ${response.error}");
        }

    }
    @Override
    final Publisher<RiverItem> publisher(final RiverConfig config) {
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
        new ObservableToPublisherAdapter<RiverItem>(
                Observable.defer(
                        {
                            Observable.merge(
                                    loadRivers().collect { river ->
                                        river.exportCatalogItemsAsRiverItems(config)
                                    }
                            ).distinct(
                                    { RiverItem item -> item.key } as Func1<RiverItem, String>
                            ).startWith(i18n)
                        } as Func0<Observable<RiverItem>>
                )
        )
    }

}
