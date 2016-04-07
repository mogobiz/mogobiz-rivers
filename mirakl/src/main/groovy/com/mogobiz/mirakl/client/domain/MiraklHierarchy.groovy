package com.mogobiz.mirakl.client.domain

import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.BulkItem

/**
 *
 * Created by smanciot on 28/03/16.
 */
class MiraklHierarchy extends MiraklItem{

    @Override
    public StringBuffer append(StringBuffer buffer) {
        buffer.append(
                String.format(
                        "$id;$label;${parent?parent.id:''};${action.toString().toLowerCase()};%n"
                )
        )
    }
}
