package com.mogobiz.mirakl.client.domain

import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.BulkItem

/**
 *
 * Created by smanciot on 28/03/16.
 */
class MiraklHierarchy extends MiraklItem{
    String label

    @Override
    public String toLine() {
        return "$id;$label;${parent?parent.id:''};${action == BulkAction.DELETE ? "delete" : "update"};";
    }
}
