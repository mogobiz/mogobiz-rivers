/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz.common.rivers

import com.mogobiz.common.client.BulkItem
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.rivers.spi.GenericRiver
import com.mogobiz.common.rivers.spi.River

/**
 *
 */
abstract class AbstractRivers<T extends River> extends AbstractGenericRivers<BulkItem, BulkResponse> implements Rivers<T> {

    /**
     * rivers
     */
    private final Map<String, T> rivers = new HashMap<String, T>()

    /**
     *
     */
    protected AbstractRivers(){
    }

    /**
     * Discover and register the available rivers
     */
    @Override
    List<GenericRiver<BulkItem, BulkResponse>> loadRivers() {
        def riverLoader = ServiceLoader.load(river())
        rivers.clear()
        riverLoader.reload()
        for(T river : riverLoader.iterator()){
            rivers.put(river.type, river)
        }
        rivers.values()
    }

    T loadRiver(String type){
        return rivers.containsKey(type) ? rivers.get(type) : null
    }

}
