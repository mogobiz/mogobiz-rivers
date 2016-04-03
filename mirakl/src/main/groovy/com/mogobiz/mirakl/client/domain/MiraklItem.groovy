package com.mogobiz.mirakl.client.domain

import com.mogobiz.common.client.BulkItem

/**
 *
 * Created by smanciot on 03/04/16.
 */
abstract class MiraklItem extends BulkItem{
    abstract String toLine()


}
