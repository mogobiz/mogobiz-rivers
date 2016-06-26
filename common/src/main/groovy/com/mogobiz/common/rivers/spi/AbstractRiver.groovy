/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.common.rivers.spi

import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.BulkItem
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.client.Item

import java.text.SimpleDateFormat

/**
 *
 */
abstract class AbstractRiver<E> extends AbstractGenericRiver<E, BulkItem, BulkResponse> implements River{

    protected AbstractRiver(){}

    abstract Item asItem(E e, RiverConfig config)

    @Override
    BulkItem asRiverItem(Object e, RiverConfig config){ // FIXME
        final String type = getType()
        final String uuid = getUuid(e as E)

        new RiverItem() {

            @Override
            String getKey(){
                "$type::$uuid"
            }

            @Override
            BulkItem asBulkItem(RiverConfig conf) {
                Item item = asItem(e as E, conf)
                def id = item.id
                item.map << [imported: formatToIso8601(new Date())]
                return new BulkItem(
                        action: BulkAction.UPDATE,
                        type : getType(),
                        id : id,
                        parent: item.parent,
                        map : item.map
                )
            }
        }.asBulkItem(config)
    }

    public String getUuid(E e) {
        e.toString()
    }

    public static String formatToIso8601(Date d) {
        new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss\'Z\'").format(d)
    }

}
