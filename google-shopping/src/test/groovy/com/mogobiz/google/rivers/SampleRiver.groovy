/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.google.rivers

import com.mogobiz.common.client.Item
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.google.client.GoogleClient
import com.mogobiz.google.rivers.spi.AbstractGoogleRiver

/**
 * Created by smanciot on 16/05/2014.
 */
class SampleRiver extends AbstractGoogleRiver<Sample> {

    @Override
    rx.Observable<Sample> retrieveCatalogItems(RiverConfig config) {
        return rx.Observable.from([
                new Sample(
                        id:1,
                        title:'Product',
                        description:'A new item available...',
                        sku:'SKU123456'
                )
        ])
    }

    @Override
    Item asItem(Sample aSample, RiverConfig config) {
        return new Item(
                id:aSample.id,
                map:[
                        title:aSample.title,
                        content:aSample.description,
                        id:aSample.sku,
                        channel:'local'
                ]
        )
    }

    @Override
    GoogleClient getClient() {
        return GoogleClient.instance
    }

    @Override
    String getType() {
        return 'sample'
    }
}

class Sample {

    Long id
    String title
    String description
    String sku

}
