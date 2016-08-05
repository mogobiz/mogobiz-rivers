/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.elasticsearch.rivers

import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.client.Item
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.elasticsearch.client.ESIndexResponse
import com.mogobiz.elasticsearch.client.ESIndexSettings
import com.mogobiz.elasticsearch.client.ESMapping
import com.mogobiz.elasticsearch.client.ESProperty
import com.mogobiz.elasticsearch.client.ESClient
import com.mogobiz.elasticsearch.rivers.mappings.ESMappings
import com.mogobiz.elasticsearch.rivers.spi.ESRiver
import rx.Observable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 *
 */
final class ESRivers extends AbstractESRivers<ESRiver> {

    static ESRivers instance

    /**
     *
     */
    private ESRivers() {  }

    static ESRivers getInstance() {
        if (instance == null) {
            instance = new ESRivers()
        }
        instance
    }

    @Override
    ESIndexResponse createCompanyIndex(RiverConfig config) {
        def mappings = []
        loadRivers().each { ESRiver river ->
            mappings << river.defineESMapping()
        }
        def clientConfig = config.clientConfig
        def conf = [debug: clientConfig?.debug]
        def credentials = clientConfig?.credentials
        if(credentials){
            conf << [username: credentials.client_id]
            conf << [password: credentials.client_secret]
        }
        ESIndexResponse response = new ESIndexResponse(acknowledged: true, error: null)
        def comments = config.clientConfig.store + '_comment'
        def exists = client.indexExists(config.clientConfig.url, comments, conf)
        if (!exists) {
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
                                    properties: []
                                            << new ESProperty(name: 'userId', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                                            << new ESProperty(name: 'externalCode', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                                            << new ESProperty(name: 'nickname', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                                            << new ESProperty(name: 'notation', type: ESClient.TYPE.INTEGER, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                                            << new ESProperty(name: 'subject', type: ESClient.TYPE.STRING, index: ESClient.INDEX.ANALYZED, multilang: false)
                                            << new ESProperty(name: 'comment', type: ESClient.TYPE.STRING, index: ESClient.INDEX.ANALYZED, multilang: false)
                                            << new ESProperty(name: 'productId', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                                            << new ESProperty(name: 'useful', type: ESClient.TYPE.LONG, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                                            << new ESProperty(name: 'notuseful', type: ESClient.TYPE.LONG, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                                            << new ESProperty(name: 'created', type: ESClient.TYPE.DATE, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
                            )],
                    conf,
                    config.languages as String[],
                    config.defaultLang)
        }
        if (response.acknowledged) {
            def wishlist = config.clientConfig.store + '_wishlist'
            exists = client.indexExists(config.clientConfig.url, wishlist, conf)
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
                wishlistsProperties << new ESProperty(name: 'externalCode', type: ESClient.TYPE.STRING, index: ESClient.INDEX.NOT_ANALYZED, multilang: false)
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
                        conf,
                        config.languages as String[],
                        config.defaultLang)
            }
        }

        if (response.acknowledged) {
            def history = config.clientConfig.store + '_history'
            exists = client.indexExists(config.clientConfig.url, history, conf)
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
                        conf,
                        config.languages as String[],
                        config.defaultLang)
            }
        }

        if (response.acknowledged) {
            def learning = config.clientConfig.store + '_learning'
            exists = client.indexExists(config.clientConfig.url, learning, conf)
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
                        conf,
                        config.languages as String[],
                        config.defaultLang)
            }
        }

        if (response.acknowledged) {
            def cart = config.clientConfig.store + '_cart'
            exists = client.indexExists(config.clientConfig.url, cart, conf)
            if (!exists) {
                response = client.createIndex(
                        config.clientConfig.url,
                        cart,
                        new ESIndexSettings(
                                number_of_replicas: config.clientConfig.config.replicas as Integer ?: 1,
                                refresh_interval: "1s"
                        ),
                        ESMappings.loadMappings("StoreCart"),
                        conf
                )
            }
            def url = config.clientConfig.url
            Set<String> indexes = client.retrieveAliasIndexes(url, 'mogobiz_carts', conf)
            if (!indexes.contains(cart)) {
                client.createAlias(conf, url, 'mogobiz_carts', cart)
            }
        }

        if (response.acknowledged) {
            def cart = config.clientConfig.store + '_predictions_view'
            exists = client.indexExists(config.clientConfig.url, cart, conf)
            if (!exists) {
                Map preditionsMap = ESMappings.loadMappings("Predictions")
                Map fisMap = ESMappings.loadMappings("FrequentPattern")
                Map allMap = new HashMap()
                allMap.putAll(preditionsMap)
                allMap.putAll(fisMap)
                response = client.createIndex(
                        config.clientConfig.url,
                        cart,
                        new ESIndexSettings(
                                number_of_replicas: config.clientConfig.config.replicas as Integer ?: 1,
                                refresh_interval: "1s"
                        ),
                        allMap,
                        conf
                )
            }
        }

        if (response.acknowledged) {
            def cart = config.clientConfig.store + '_predictions_purchase'
            exists = client.indexExists(config.clientConfig.url, cart, conf)
            if (!exists) {
                Map preditionsMap = ESMappings.loadMappings("Predictions")
                Map fisMap = ESMappings.loadMappings("FrequentPattern")
                Map allMap = new HashMap()
                allMap.putAll(preditionsMap)
                allMap.putAll(fisMap)
                response = client.createIndex(
                        config.clientConfig.url,
                        cart,
                        new ESIndexSettings(
                                number_of_replicas: config.clientConfig.config.replicas as Integer ?: 1,
                                refresh_interval: "1s"
                        ),
                        allMap,
                        conf
                )
            }
        }

        if (response.acknowledged) {
            def bo = config.clientConfig.store + '_bo'
            exists = client.indexExists(config.clientConfig.url, bo, conf)
            if (!exists) {
                Map cartMap = ESMappings.loadMappings("BOCart")
                Map itemMap = ESMappings.loadMappings("BOCartItemEx")
                Map allMap = new HashMap()
                allMap.putAll(cartMap)
                allMap.putAll(itemMap)
                response = client.createIndex(
                        config.clientConfig.url,
                        bo,
                        new ESIndexSettings(
                                number_of_replicas: config.clientConfig.config.replicas as Integer ?: 1,
                                refresh_interval: "1s"
                        ),
                        allMap,
                        conf
                )
            }
        }

        if (response.acknowledged) {
            final url = config.clientConfig.url
            final index = config.clientConfig.config.index as String
            final settings = new ESIndexSettings(number_of_replicas: 0, refresh_interval: "-1")
            exists = client.indexExists(config.clientConfig.url, index, conf)
            if(!exists){
                response = client.createIndex(
                        url,
                        index,
                        settings,
                        mappings,
                        conf,
                        config.languages as String[],
                        config.defaultLang)
            }
            else{
                client.updateIndex(url, index, settings, conf)
            }
        }

        response
    }

    @Override
    protected Collection<Observable<Future<BulkResponse>>> iterable(RiverConfig config, ExecutionContext ec) {
        Collection<Observable<Future<BulkResponse>>> iterable = []
        iterable << client.upsert(config, [new Item(id: 1L, type: 'i18n', map: ['languages': config.languages])], ec)
        loadRivers().each { river ->
            iterable << river.exportCatalogItems(config, ec)
        }
        iterable
    }

    @Override
    Class<ESRiver> river() {
        ESRiver.class
    }

}
