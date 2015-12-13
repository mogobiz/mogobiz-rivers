/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.elasticsearch.rivers.spi

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
}
