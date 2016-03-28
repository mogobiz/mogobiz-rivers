package com.mogobiz.mirakl.client

import com.mogobiz.common.client.BulkAction
import com.mogobiz.mirakl.client.domain.MiraklCategory

/**
 *
 * Created by smanciot on 27/03/16.
 */
class MiraklCategoryTest extends GroovyTestCase{

    void testMiraklCategory(){
        def category1 = new MiraklCategory(
                id: 'category1',
                label:'category1Label',
                logisticClass: 'A',
                action: BulkAction.DELETE,
                parent: null
        )
        log.info(category1.toString())
        assertEquals("category1;category1Label;A;delete;", category1.toString())
        def category2 = new MiraklCategory(
                id: 'category2',
                label:'category2Label',
                logisticClass: 'A',
                action: BulkAction.DELETE,
                parent: category1
        )
        log.info(category2.toString())
        assertEquals("category2;category2Label;A;delete;category1", category2.toString())
    }
}
