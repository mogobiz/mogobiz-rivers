package com.mogobiz.common.rivers.spi

import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.BulkItem
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.client.Item
import com.mogobiz.common.client.Client

import rx.functions.Func1
import rx.internal.reactivestreams.ObservableToPublisherAdapter

import org.reactivestreams.Publisher

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import java.text.SimpleDateFormat

/**
 * Created by smanciot on 14/05/2014.
 */
abstract class AbstractRiver<E, T extends Client> implements River {

    protected AbstractRiver(){}

    final Publisher<RiverItem> exportCatalogItemsAsPublisher(final RiverConfig config){
        new ObservableToPublisherAdapter<RiverItem>(exportCatalogItemsAsRiverItems(config))
    }

    final rx.Observable<RiverItem> exportCatalogItemsAsRiverItems(final RiverConfig config){
        final Map<String, Item> previousCatalogItems = [:]
        retrieveCatalogPreviousItems(config)?.hits?.each {item ->
            def id = item.id
            if(id){
                previousCatalogItems.put(id, item)
            }
        }
        retrieveCatalogItems(config).flatMap({Object e ->
            rx.Observable.just(asRiverItem(e as E, previousCatalogItems))
        }as Func1<Object, rx.Observable<RiverItem>>)
    }

    /**
     * @deprecated
     * @param config - river configuration
     * @param ec - execution context
     * @param count - bulk size
     * @return rx.Observable
     */
    @Override
    final rx.Observable<Future<BulkResponse>> exportCatalogItems(
            final RiverConfig config,
            final ExecutionContext ec,
            final int count = 100){
        final Map<String, Item> previousCatalogItems = [:]
        retrieveCatalogPreviousItems(config)?.hits?.each {item ->
            def id = item.id
            if(id){
                previousCatalogItems.put(id, item)
            }
        }
        retrieveCatalogItems(config).flatMap({Object e ->
            def item = asItem(e as E, config)
            def id = item.id
            def previous = previousCatalogItems.get(id)
            item = updateItemWithPrevious(item, previous)
            item.map << [imported: formatToIso8601(new Date())]
            rx.Observable.just(new BulkItem(
                    action: previous ? BulkAction.UPDATE : BulkAction.INSERT,
                    type : getType(),
                    id : id,
                    parent: item.parent,
                    map : item.map
            ))
        }as Func1<Object, rx.Observable<BulkItem>>).buffer(count).flatMap({items ->
            rx.Observable.just(getClient().bulk(config, items as List<BulkItem>, ec))
        }as Func1<List<BulkItem>, rx.Observable<Future<BulkResponse>>>)
    }

    final rx.Observable<Future<BulkResponse>> upsertCatalogObjects(
            final RiverConfig config,
            final Collection<E> objects,
            final ExecutionContext ec = null,
            final int count = 100){
        rx.Observable.from(objects).map({e ->
            def item = asItem(e as E, config)
            item.map << [imported: formatToIso8601(new Date())]
            def id = item.id as Long
            rx.Observable.just(new BulkItem(
                    type : getType(),
                    action: id && id > 0 ? BulkAction.UPDATE : BulkAction.INSERT,
                    id: id,
                    parent: item.parent,
                    map: id && id > 0 ? [doc: item.map, doc_as_upsert : true] : item.map
            ))
        }as Func1<Object, rx.Observable<BulkItem>>).buffer(count).flatMap({items ->
            rx.Observable.just(getClient().bulk(config, items as List<BulkItem>, ec))
        }as Func1<List<BulkItem>, rx.Observable<Future<BulkResponse>>>)
    }

    abstract rx.Observable<E> retrieveCatalogItems(final RiverConfig config)

    abstract Item asItem(E e, RiverConfig config)

    final RiverItem asRiverItem(E e, final Map<String, Item> previousCatalogItems = [:]){
        final String type = getType()
        final String uuid = getUuid(e)

        new RiverItem() {

            @Override
            String getKey(){
                "$type::$uuid"
            }

            @Override
            BulkItem asBulkItem(RiverConfig config) {
                Item item = asItem(e as E, config)
                def id = item.id
                def previous = previousCatalogItems.get(id)
                item = updateItemWithPrevious(item, previous)
                item.map << [imported: formatToIso8601(new Date())]
                return new BulkItem(
                        action: previous ? BulkAction.UPDATE : BulkAction.INSERT,
                        type : getType(),
                        id : id,
                        parent: item.parent,
                        map : item.map
                )
            }
        }
    }

    def String getUuid(E e){
        e.toString()
    }

    protected Item updateItemWithPrevious(Item item, Item previous){
        previous?.map?.each {k, v ->
            item.map.put(k, v)
        }
        item
    }

    def List<String> previousProperties(){
        ['id'] as List<String>
    }

    final static String formatToIso8601(Date d){
        new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ss\'Z\'').format(d)
    }

    abstract T getClient()
}
