/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.elasticsearch.rivers.spi

import com.mogobiz.common.client.Item
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.elasticsearch.client.ESMapping
import com.mogobiz.elasticsearch.rivers.mappings.ESMappings

/**
 *
 */
abstract class AbstractESBORiver<E> extends AbstractESRiver<E> implements ESBORiver{

    protected AbstractESBORiver(){}

    @Override
    final ESMapping defineESMapping() {
        return null
    }

    @Override
    final Map defineESMappingAsMap() {
        ESMappings.loadMappings(getType())
    }

    @Override
    abstract Item asItem(E e, RiverConfig config)

    @Override
    String getUuid(E e) {
      super.getUuid(e)
    }
}
