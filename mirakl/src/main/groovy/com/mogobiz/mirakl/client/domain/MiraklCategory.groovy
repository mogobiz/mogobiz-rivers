package com.mogobiz.mirakl.client.domain

import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.BulkItem

/**
 *
 * Created by smanciot on 28/03/16.
 */
class MiraklCategory extends BulkItem{
    String label
    String logisticClass = ''

    @Override
    public String toString() {
        return "$id;$label;$logisticClass;${action == BulkAction.DELETE ? "delete" : "update"};${parent?parent.id:''}";
    }
}
