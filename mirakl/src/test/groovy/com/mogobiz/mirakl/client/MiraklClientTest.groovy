package com.mogobiz.mirakl.client

import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.ClientConfig
import com.mogobiz.common.client.Credentials
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.mirakl.client.domain.MiraklCategory
import com.mogobiz.mirakl.client.io.SearchShopsRequest
import com.mogobiz.mirakl.client.io.Synchronization

/**
 *
 * Created by smanciot on 27/03/16.
 */
class MiraklClientTest extends GroovyTestCase{

    def MIRAKL_API_KEY = '096401e5-c3e8-42fe-9891-ed94cd4c1a89'

//    def FRONT_KEY = 'de901fbc-804e-4733-a5bd-8765c41c921f'
//    def OPERATOR_KEY = '3c404360-7b63-4f34-beb4-2d564fde2e03'

    def MIRAKL_URL = 'https://ebiznext-dev.mirakl.net'

    void testSynchronizeCategories(){
        def clientConfig = new ClientConfig(url: MIRAKL_URL, credentials: new Credentials(apiKey: MIRAKL_API_KEY))
        def riverConfig = new RiverConfig(clientConfig: clientConfig)
        def categories = []
        (1..10).each {
            categories << createCategory(it)
        }
        def synchronization = MiraklClient.synchronizeCategories(riverConfig, categories)
        assertNotNull(synchronization)
        def synchro = synchronization.synchroId
        assertNotNull(synchro)
        log.info(synchro.toString())
        def synchronizationStatusResponse = MiraklClient.refreshCategoriesSynchronizationStatus(riverConfig, synchro)
        assertNotNull(synchronizationStatusResponse)
        assertFalse(synchronizationStatusResponse.hasErrorReport as Boolean)
    }

    void testSearchShops(){
        def clientConfig = new ClientConfig(url: MIRAKL_URL, credentials: new Credentials(apiKey: MIRAKL_API_KEY))
        def riverConfig = new RiverConfig(clientConfig: clientConfig)
        def searchShopsResponse = MiraklClient.searchShops(riverConfig, new SearchShopsRequest())
        assertNotNull(searchShopsResponse)
    }

    private static MiraklCategory createCategory(
            int indice,
            String logisticClass = 'A',
            BulkAction action = BulkAction.INSERT,
            MiraklCategory parent = null){
        return new MiraklCategory(
                id: "category$indice",
                label: "category${indice}Label",
                logisticClass: logisticClass,
                action: action,
                parent: parent
        )
    }
}
