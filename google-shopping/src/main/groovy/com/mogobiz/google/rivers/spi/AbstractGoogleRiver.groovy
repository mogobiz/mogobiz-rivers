package com.mogobiz.google.rivers.spi

import com.mogobiz.common.client.Item
import com.mogobiz.common.client.Request
import com.mogobiz.common.client.SearchResponse
import com.mogobiz.common.rivers.spi.AbstractRiver
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.google.client.GoogleClient

/**
 * Created by smanciot on 16/05/2014.
 */
abstract class AbstractGoogleRiver<E> extends AbstractRiver<E, GoogleClient> implements GoogleRiver{

    @Override
    final SearchResponse retrieveCatalogPreviousItems(final RiverConfig config){
        getClient().search(new Request(clientConfig: config.clientConfig))
    }

    @Override
    final protected Item updateItemWithPrevious(Item item, Item previous) {
        previous?.findAll {k, v -> (k as String) in previousProperties()}?.each {k, v ->
            item.map.put(k, v)
        }
        item
    }

}
