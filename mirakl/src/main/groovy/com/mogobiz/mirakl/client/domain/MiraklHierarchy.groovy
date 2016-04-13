package com.mogobiz.mirakl.client.domain

import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.BulkItem

/**
 *
 * Created by smanciot on 28/03/16.
 */
class MiraklHierarchy extends MiraklItem{

    @Override
    public StringBuffer append(StringBuffer buffer, String separator = ";") {
        buffer.append(
                String.format(
                        "$id$separator$label$separator${parent?parent.id:''}$separator${action.toString().toLowerCase()}$separator%n"
                )
        )
    }
}
