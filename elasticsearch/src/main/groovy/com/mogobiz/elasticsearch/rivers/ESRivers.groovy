package com.mogobiz.elasticsearch.rivers

import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.client.Item
import com.mogobiz.common.rivers.AbstractRiverCache
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.common.rivers.Rivers
import com.mogobiz.elasticsearch.client.ESIndexResponse
import com.mogobiz.elasticsearch.client.ESIndexSettings
import com.mogobiz.elasticsearch.client.ESMapping
import com.mogobiz.elasticsearch.client.ESProperty
import com.mogobiz.elasticsearch.client.ESClient
import com.mogobiz.elasticsearch.rivers.spi.ESRiver
import rx.functions.Action1
import rx.Observable
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
                            number_of_replicas: config.clientConfig.config.replicas ?: 1,
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
                                number_of_replicas: config.clientConfig.config.replicas ?: 1,
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
                                number_of_replicas: config.clientConfig.config.replicas ?: 1,
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
                                number_of_replicas: config.clientConfig.config.replicas ?: 1,
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

        Collection<Future<BulkResponse>> collection = []

        if(response.acknowledged){
            Collection<Observable<Future<BulkResponse>>> iterable = []

            iterable << client.upsert(config, [new Item(id:1L, type:'i18n', map: ['languages':config.languages])], ec)

            loadRivers().each {river ->
                iterable << river.exportCatalogItems(config, ec, 100)
            }
            Observable.merge(iterable).subscribe({
                collection << (it as Future<BulkResponse>)
            }as Action1<Future<BulkResponse>>,
            {th -> th.printStackTrace(System.err)} as Action1<Throwable>)
        }

        collect(collection, ec)
    }

}
