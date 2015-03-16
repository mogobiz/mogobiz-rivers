package com.mogobiz.elasticsearch.rivers

import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.BulkItem
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.client.Item
import com.mogobiz.common.rivers.AbstractRiverCache
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.common.rivers.Rivers
import com.mogobiz.common.rivers.spi.RiverItem
import com.mogobiz.elasticsearch.client.ESIndexResponse
import com.mogobiz.elasticsearch.client.ESIndexSettings
import com.mogobiz.elasticsearch.client.ESMapping
import com.mogobiz.elasticsearch.client.ESProperty
import com.mogobiz.elasticsearch.client.ESClient
import com.mogobiz.elasticsearch.rivers.mappings.ESMappings
import com.mogobiz.elasticsearch.rivers.spi.ESRiver
import org.reactivestreams.Publisher
import rx.functions.Action1
import rx.Observable
import rx.functions.Func0
import rx.functions.Func1
import rx.internal.reactivestreams.ObservableToPublisherAdapter
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 * Created by stephane.manciot@ebiznext.com on 16/02/2014.
 */
final class ESRivers extends Rivers<ESRiver>{

    static ESRivers instance

    static final ESClient client = ESClient.instance

    /**
     *
     */
    private ESRivers(){super(ESRiver.class)}

    static ESRivers getInstance(){
        if(instance == null){
            instance = new ESRivers()
        }
        instance
    }

    ESIndexResponse createCompanyIndex(RiverConfig config){
        def mappings = []
        loadRivers().each {river ->
            mappings << river.defineESMapping()
        }
        ESIndexResponse response = new ESIndexResponse(acknowledged: true, error: null)
        def comments = config.clientConfig.store + '_comment'
        def exists = client.indexExists(config.clientConfig.url, comments)
        if(!exists){
            response = client.createIndex(
                    config.clientConfig.url,
                    comments,
                    new ESIndexSettings(
                            number_of_replicas: config.clientConfig.config.replicas as Integer ?: 1,
                            refresh_interval: "1s"
                    ),
                    [
                        new ESMapping(
                            timestamp: true,
                            type: 'comment',
                            properties : []
                                << new ESProperty(name:'userUuid', type:ESClient.TYPE.STRING, index:ESClient.INDEX.NOT_ANALYZED, multilang:false)
                                << new ESProperty(name:'externalCode', type:ESClient.TYPE.STRING, index:ESClient.INDEX.NOT_ANALYZED, multilang:false)
                                << new ESProperty(name:'nickname', type:ESClient.TYPE.STRING, index:ESClient.INDEX.NOT_ANALYZED, multilang:false)
                                << new ESProperty(name:'notation', type:ESClient.TYPE.INTEGER, index:ESClient.INDEX.NOT_ANALYZED, multilang:false)
                                << new ESProperty(name:'subject', type:ESClient.TYPE.STRING, index:ESClient.INDEX.ANALYZED, multilang:false)
                                << new ESProperty(name:'comment', type:ESClient.TYPE.STRING, index:ESClient.INDEX.ANALYZED, multilang:false)
                                << new ESProperty(name:'productUuid', type:ESClient.TYPE.STRING, index:ESClient.INDEX.NOT_ANALYZED, multilang:false)
                                << new ESProperty(name:'useful', type:ESClient.TYPE.LONG, index:ESClient.INDEX.NOT_ANALYZED, multilang:false)
                                << new ESProperty(name:'notuseful', type:ESClient.TYPE.LONG, index:ESClient.INDEX.NOT_ANALYZED, multilang:false)
                                << new ESProperty(name:'created', type:ESClient.TYPE.DATE, index:ESClient.INDEX.NOT_ANALYZED, multilang:false)
                        )],
                    [debug:config.debug],
                    config.languages as String[],
                    config.defaultLang)
        }
        if(response.acknowledged) {
            def wishlist = config.clientConfig.store + '_wishlist'
            exists = client.indexExists(config.clientConfig.url, wishlist)
            if (!exists) {
                def ownerProperties = []
                ownerProperties << new ESProperty(name: 'dayOfBirth', type: ESClient.TYPE.INTEGER, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                ownerProperties << new ESProperty(name: 'monthOfBirth', type: ESClient.TYPE.INTEGER, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                ownerProperties << new ESProperty(name: 'description', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                ownerProperties << new ESProperty(name: 'email', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                ownerProperties << new ESProperty(name: 'name', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)

                def brandsProperties = []
                brandsProperties << new ESProperty(name: 'uuid', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                brandsProperties << new ESProperty(name: 'name', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)

                def categoriesProperties = []
                categoriesProperties << new ESProperty(name: 'uuid', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                categoriesProperties << new ESProperty(name: 'name', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)

                def ideasProperties = []
                ideasProperties << new ESProperty(name: 'uuid', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                ideasProperties << new ESProperty(name: 'name', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)

                def itemsProperties = []
                itemsProperties << new ESProperty(name: 'uuid', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                itemsProperties << new ESProperty(name: 'name', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                itemsProperties << new ESProperty(name: 'product', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                itemsProperties << new ESProperty(name: 'sku', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)

                def wishlistsProperties = []
                wishlistsProperties << new ESProperty(name: 'uuid', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                wishlistsProperties << new ESProperty(name:'externalCode', type:ESClient.TYPE.STRING, index:ESClient.INDEX.NOT_ANALYZED, multilang:false)
                wishlistsProperties << new ESProperty(name: 'dateCreated', type: ESClient.TYPE.DATE, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                wishlistsProperties << new ESProperty(name: 'lastUpdated', type: ESClient.TYPE.DATE, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                wishlistsProperties << new ESProperty(name: 'alert', type: ESClient.TYPE.BOOLEAN, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                wishlistsProperties << new ESProperty(name: 'default', type: ESClient.TYPE.BOOLEAN, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                wishlistsProperties << new ESProperty(name: 'name', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                wishlistsProperties << new ESProperty(name: 'token', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                wishlistsProperties << new ESProperty(name: 'visibility', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                wishlistsProperties << new ESProperty(name: 'brands', type: ESClient.TYPE.NESTED, properties: brandsProperties)
                wishlistsProperties << new ESProperty(name: 'categories', type: ESClient.TYPE.NESTED, properties: categoriesProperties)
                wishlistsProperties << new ESProperty(name: 'ideas', type: ESClient.TYPE.NESTED, properties: ideasProperties)
                wishlistsProperties << new ESProperty(name: 'items', type: ESClient.TYPE.NESTED, properties: itemsProperties)

                response = client.createIndex(
                        config.clientConfig.url,
                        wishlist,
                        new ESIndexSettings(
                                number_of_replicas: config.clientConfig.config.replicas as Integer ?: 1,
                                refresh_interval: "1s"
                        ),
                        [
                                new ESMapping(
                                        timestamp: true,
                                        type: 'wishlistlist',
                                        properties: []
                                                << new ESProperty(name: 'uuid', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                                                << new ESProperty(name: 'dateCreated', type: ESClient.TYPE.DATE, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                                                << new ESProperty(name: 'lastUpdated', type: ESClient.TYPE.DATE, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                                                << new ESProperty(name: 'owner', type: ESClient.TYPE.NESTED, properties: ownerProperties)
                                                << new ESProperty(name: 'wishlists', type: ESClient.TYPE.NESTED, properties: wishlistsProperties)
                                )],
                        [debug: config.debug],
                        config.languages as String[],
                        config.defaultLang)
            }
        }

        if(response.acknowledged) {
            def history = config.clientConfig.store + '_history'
            exists = client.indexExists(config.clientConfig.url, history)
            if (!exists) {
                response = client.createIndex(
                        config.clientConfig.url,
                        history,
                        new ESIndexSettings(
                                number_of_replicas: config.clientConfig.config.replicas as Integer ?: 1,
                                refresh_interval: "1s"
                        ),
                        [
                                new ESMapping(
                                        timestamp: true,
                                        type: 'history',
                                        properties: []
                                                << new ESProperty(name: 'productIds', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                                )],
                        [debug: config.debug],
                        config.languages as String[],
                        config.defaultLang)
            }
        }

        if(response.acknowledged){
            def learning = config.clientConfig.store + '_learning'
            exists = client.indexExists(config.clientConfig.url, learning)
            if (!exists) {
                response = client.createIndex(
                        config.clientConfig.url,
                        learning,
                        new ESIndexSettings(
                                number_of_replicas: config.clientConfig.config.replicas as Integer ?: 1,
                                refresh_interval: "1s"
                        ),
                        [
                                new ESMapping(
                                        timestamp: true,
                                        type: 'cartaction',
                                        properties: []
                                                << new ESProperty(name: 'trackingid', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                                                << new ESProperty(name: 'itemids', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                                ),
                                new ESMapping(
                                        timestamp: true,
                                        type: 'useritemaction',
                                        properties: []
                                                << new ESProperty(name: 'trackingid', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                                                << new ESProperty(name: 'itemid', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                                                << new ESProperty(name: 'action', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                                )
                        ],
                        [debug: config.debug],
                        config.languages as String[],
                        config.defaultLang)
            }
        }

        if(response.acknowledged){
            def cart = config.clientConfig.store + '_cart'
            exists = client.indexExists(config.clientConfig.url, cart)
            if(!exists){
                response = client.createIndex(
                        config.clientConfig.url,
                        cart,
                        new ESIndexSettings(
                                number_of_replicas: config.clientConfig.config.replicas as Integer ?: 1,
                                refresh_interval: "1s"
                        ),
                        ESMappings.loadMappings("StoreCart"),
                        [debug:config.debug])
            }
            def url = config.clientConfig.url
            Set<String> indexes = client.retrieveAliasIndexes(url, 'mogobiz_carts', [debug: true])
            if(!indexes.contains(cart)){
                client.createAlias([debug: true], url, 'mogobiz_carts', cart)
            }
        }

        if(response.acknowledged){
            def bo = config.clientConfig.store + '_bo'
            exists = client.indexExists(config.clientConfig.url, bo)
            if(!exists){
                response = client.createIndex(
                        config.clientConfig.url,
                        bo,
                        new ESIndexSettings(
                                number_of_replicas: config.clientConfig.config.replicas as Integer ?: 1,
                                refresh_interval: "1s"
                        ),
                        ESMappings.loadMappings("BOCart"),
                        [debug:config.debug])
            }
        }

        if(response.acknowledged){
            response = client.createIndex(
                    config.clientConfig.url,
                    config.clientConfig.config.index as String,
                    new ESIndexSettings(number_of_replicas: 0, refresh_interval: "-1"),
                    mappings,
                    [debug:config.debug],
                    config.languages as String[],
                    config.defaultLang)
        }

        response
    }

    @Override
    Future<Collection<BulkResponse>> export(
            RiverConfig config,
            ExecutionContext ec){

        AbstractRiverCache.purgeAll()

        ESIndexResponse response = createCompanyIndex(config)

        if(response.acknowledged){
            Collection<Future<BulkResponse>> collection = []

            Collection<Observable<Future<BulkResponse>>> iterable = []

            iterable << client.upsert(config, [new Item(id:1L, type:'i18n', map: ['languages':config.languages])], ec)

            loadRivers().each {river ->
                iterable << river.exportCatalogItems(config, ec, 100)
            }
            Observable.merge(iterable).subscribe({
                collection << (it as Future<BulkResponse>)
            }as Action1<Future<BulkResponse>>,
            {th -> th.printStackTrace(System.err)} as Action1<Throwable>)

            collect(collection, ec)
        }
        else{
            throw new Exception("an error occured while creating index ${response.error}");
        }

    }

    @Override
    Publisher<RiverItem> publisher(final RiverConfig config){
        final RiverItem i18n = new RiverItem() {

            @Override
            String getKey() {
                return "i18n::1"
            }

            @Override
            BulkItem asBulkItem(RiverConfig c) {
                new BulkItem(
                        type : 'i18n',
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
                                    loadRivers().collect {river ->
                                        river.exportCatalogItemsAsRiverItems(config)
                                    }
                            ).distinct(
                                    { RiverItem item -> item.key }as Func1<RiverItem, String>
                            ).startWith(i18n)
                        }as Func0<Observable<RiverItem>>
                )
        )
    }

}
