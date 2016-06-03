/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.elasticsearch.rivers.spi

import com.mogobiz.common.client.Item
import com.mogobiz.common.client.SearchResponse
import com.mogobiz.common.rivers.spi.AbstractRiver
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.elasticsearch.client.ESClient
import com.mogobiz.elasticsearch.client.ESRequest

/**
 *
 */
abstract class AbstractESRiver<E> extends AbstractRiver<E, ESClient> implements ESRiver{

    protected AbstractESRiver(){}

    @Override
    final SearchResponse retrieveCatalogPreviousItems(RiverConfig config){
        final properties = previousProperties()
        def hits = []
        if(!properties?.isEmpty()){
            if(!properties.contains("id")){
                properties << "id"
            }
            ESRequest request = new ESRequest(
                    url:config.clientConfig.url,
                    index:config.clientConfig.store,
                    type: getType(),
                    query:[:],
                    included: properties,
                    excluded: [])
            def conf = [debug:config.clientConfig?.debug]
            def credentials = config.clientConfig?.credentials
            if(credentials){
                conf << [username: credentials.client_id]
                conf << [password: credentials.client_secret]
            }
            hits.addAll(getClient().search(request, conf).hits)
        }
        return new SearchResponse(hits:hits?.collect {hit ->
            new Item(id:hit.id, type: getType(), map:hit)
        }, total:hits?.size())
    }

    ESClient getClient(){
        return ESClient.instance
    }
}
