package com.mogobiz.mirakl.client

import com.mogobiz.common.client.BulkAction
import com.mogobiz.mirakl.client.domain.MiraklApi
import com.mogobiz.mirakl.client.domain.MiraklCategory

import static com.mogobiz.tools.ScalaTools.*

/**
 *
 * Created by smanciot on 27/03/16.
 */
class MiraklCategoryTest extends GroovyTestCase{

    void testMiraklCategory(){
        def category1 = new MiraklCategory(
                'category1',
                'category1Label',
                BulkAction.DELETE,
                toScalaOption(null),
                'A',
                '',
                toScalaOption(null),
                toScalaOption(null)
        )
        log.info(category1.append(new StringBuffer(), ";", MiraklApi.categoriesHeader()).toString())
        assertEquals("category1;category1Label;A;delete;;;\n", category1.append(new StringBuffer(), ";", MiraklApi.categoriesHeader()).toString())
        def category2 = new MiraklCategory(
                'category2',
                'category2Label',
                BulkAction.DELETE,
                toScalaOption(category1),
                'A',
                '',
                toScalaOption(null),
                toScalaOption(null)
        )
        log.info(category2.append(new StringBuffer(), ";", MiraklApi.categoriesHeader()).toString())
        assertEquals("category2;category2Label;A;delete;category1;;\n", category2.append(new StringBuffer(), ";", MiraklApi.categoriesHeader()).toString())
    }
}
