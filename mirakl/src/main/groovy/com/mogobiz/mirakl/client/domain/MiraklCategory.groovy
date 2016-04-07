package com.mogobiz.mirakl.client.domain

import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.BulkItem

/**
 *
 * Created by smanciot on 28/03/16.
 */
class MiraklCategory extends MiraklItem{

    String logisticClass = ''

    @Override
    public StringBuffer append(StringBuffer buffer) {
        buffer.append(
                String.format(
                        "$id;$label;$logisticClass;${action.toString().toLowerCase()};${parent?parent.id:''}%n"
                )
        )
    }
}
