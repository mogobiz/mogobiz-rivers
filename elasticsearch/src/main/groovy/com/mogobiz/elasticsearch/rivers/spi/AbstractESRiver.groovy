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
 * Created by stephane.manciot@ebiznext.com on 05/03/2014.
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
            hits.addAll(getClient().search(request, [debug:config.clientConfig.debug]).hits)
        }
        return new SearchResponse(hits:hits?.collect {hit ->
            new Item(id:hit.id, type: getType(), map:hit)
        }, total:hits?.size())
    }

    ESClient getClient(){
        return ESClient.instance
    }
}
