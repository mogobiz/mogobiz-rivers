package com.mogobiz.mirakl.client

import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.ClientConfig
import com.mogobiz.common.client.Credentials
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.mirakl.client.domain.MiraklAttribute
import com.mogobiz.mirakl.client.domain.MiraklCategory
import com.mogobiz.mirakl.client.domain.MiraklHierarchy
import com.mogobiz.mirakl.client.domain.MiraklValue
import com.mogobiz.mirakl.client.domain.SynchronizationStatus
import com.mogobiz.mirakl.client.io.SearchShopsRequest

import static com.mogobiz.tools.ScalaTools.*

/**
 *
 * Created by smanciot on 27/03/16.
 */
class MiraklClientTest extends GroovyTestCase{

    def FRONT_API_KEY = '096401e5-c3e8-42fe-9891-ed94cd4c1a89' //front api key

    def API_KEY = '75d5edfa-94a1-478b-aad4-dbf9c37ef70e' //shop api key

    def SHOP_ID = "2002"

    def MIRAKL_URL = 'https://ebiznext-dev.mirakl.net'

    def waitingStatus = [SynchronizationStatus.WAITING, SynchronizationStatus.RUNNING, SynchronizationStatus.QUEUED]

    void testSynchronizeCategories(){
        RiverConfig riverConfig = riverConfig()
        def categories = []
        (1..10).each {
            categories << createCategory(it)
        }
        def synchronization = MiraklClient.synchronizeCategories(riverConfig, categories)
        assertNotNull(synchronization)
        def synchro = synchronization.synchroId
        assertNotNull(synchro)
        def synchronizationStatusResponse = MiraklClient.refreshCategoriesSynchronizationStatus(riverConfig, synchro)
        assertNotNull(synchronizationStatusResponse)
        assertFalse(synchronizationStatusResponse.hasErrorReport)
        while(synchronizationStatusResponse.status in waitingStatus){
            Thread.sleep(1000)
            synchronizationStatusResponse = MiraklClient.refreshCategoriesSynchronizationStatus(riverConfig, synchro)
        }
        assertEquals(SynchronizationStatus.COMPLETE, synchronizationStatusResponse.status)
        if(synchronizationStatusResponse.hasErrorReport){
            fail(MiraklClient.loadCategoriesSynchronizationErrorReport(riverConfig, synchro))
        }
    }

    void testSearchShops(){
        RiverConfig riverConfig = riverConfig()
        def searchShopsResponse = MiraklClient.searchShops(riverConfig, new SearchShopsRequest())
        assertNotNull(searchShopsResponse)
        assertTrue(searchShopsResponse.shops?.size() > 0)
    }

    void testListHierarchies(){
        RiverConfig riverConfig = riverConfig()
        def hierarchiesResponse = MiraklClient.listHierarchies(riverConfig)
        assertNotNull(hierarchiesResponse)
    }

    void testImportHierarchies(){
        RiverConfig riverConfig = riverConfig()
        def hierarchies = MiraklClient.listHierarchies(riverConfig).hierarchies.collect {new MiraklHierarchy(it)}
        def importResponse = MiraklClient.importHierarchies(riverConfig, hierarchies)
        assertNotNull(importResponse)
        def trackingId = importResponse.importId
        assertNotNull(trackingId)
        def trackingImportStatus = MiraklClient.trackHierarchiesImportStatusResponse(riverConfig, trackingId)
        assertNotNull(trackingImportStatus)
        assertFalse(trackingImportStatus.hasErrorReport)
        while(trackingImportStatus.importStatus in waitingStatus){
            Thread.sleep(1000)
            trackingImportStatus = MiraklClient.trackHierarchiesImportStatusResponse(riverConfig, trackingId)
        }
        assertEquals(SynchronizationStatus.COMPLETE, trackingImportStatus.importStatus)
        if(trackingImportStatus.hasErrorReport){
            fail(MiraklClient.loadHierarchiesSynchronizationErrorReport(riverConfig, trackingId))
        }
    }

    void testListValues(){
        RiverConfig riverConfig = riverConfig()
        def listValuesResponse = MiraklClient.listValues(riverConfig)
        assertNotNull(listValuesResponse)
    }

    void testImportListOfValues(){
        RiverConfig riverConfig = riverConfig()
        def listOfValues = []
        (1..5).each {
            listOfValues.addAll(createValues(it))
        }
        def importValuesResponse = MiraklClient.importValues(riverConfig, listOfValues)
        assertNotNull(importValuesResponse)
        def trackingId = importValuesResponse.importId
        assertNotNull(trackingId)
        def trackingImportStatus = MiraklClient.trackValuesImportStatusResponse(riverConfig, trackingId)
        assertNotNull(trackingImportStatus)
        assertFalse(trackingImportStatus.hasErrorReport)
        while(trackingImportStatus.importStatus in waitingStatus){
            Thread.sleep(1000)
            trackingImportStatus = MiraklClient.trackValuesImportStatusResponse(riverConfig, trackingId)
        }
        assertEquals(SynchronizationStatus.COMPLETE, trackingImportStatus.importStatus)
        if(trackingImportStatus.hasErrorReport){
            fail(MiraklClient.loadValuesSynchronizationErrorReport(riverConfig, trackingId))
        }
    }

    void testListAttributes(){
        RiverConfig riverConfig = riverConfig()
        def listAttributesResponse = MiraklClient.listAttributes(riverConfig)
        assertNotNull(listAttributesResponse)
        assertTrue(listAttributesResponse?.attributes?.size() > 0)
    }

    void testImportAttributes(){
        RiverConfig riverConfig = riverConfig()
        def attributes = MiraklClient.listAttributes(riverConfig).attributes.collect {new MiraklAttribute(it)}
        def importValuesResponse = MiraklClient.importAttributes(riverConfig, attributes)
        assertNotNull(importValuesResponse)
        def trackingId = importValuesResponse.importId
        assertNotNull(trackingId)
        def trackingImportStatus = MiraklClient.trackAttributesImportStatusResponse(riverConfig, trackingId)
        assertNotNull(trackingImportStatus)
        assertFalse(trackingImportStatus.hasErrorReport)
        while(trackingImportStatus.importStatus in waitingStatus){
            Thread.sleep(1000)
            trackingImportStatus = MiraklClient.trackAttributesImportStatusResponse(riverConfig, trackingId)
        }
        assertEquals(SynchronizationStatus.COMPLETE, trackingImportStatus.importStatus)
        if(trackingImportStatus.hasErrorReport){
            fail(MiraklClient.loadAttributesSynchronizationErrorReport(riverConfig, trackingId))
        }
    }

    private RiverConfig riverConfig(String frontKey = FRONT_API_KEY, String apiKey = API_KEY) {
        def clientConfig = new ClientConfig(merchant_id: SHOP_ID, merchant_url: MIRAKL_URL, credentials: new Credentials(frontKey: frontKey, apiKey: apiKey))
        def riverConfig = new RiverConfig(clientConfig: clientConfig)
        riverConfig
    }

    private static MiraklCategory createCategory(
            int indice,
            String logisticClass = 'A',
            BulkAction action = BulkAction.INSERT,
            MiraklCategory parent = null){
        return new MiraklCategory(
                "category$indice",
                "category${indice}Label",
                action,
                toScalaOption(parent),
                logisticClass,
                ''
        )
    }

    private static MiraklHierarchy createHierarchy(
            int indice,
            BulkAction action = BulkAction.INSERT,
            MiraklCategory parent = null){
        return new MiraklHierarchy(
                "hierarchy$indice",
                "hierarchy${indice}Label",
                action,
                toScalaOption(parent)
        )
    }

    private static List<MiraklValue> createValues(
            int indice){
        def ret = []
        def parent = new MiraklValue(
                "values$indice",
                "values${indice}Label"
        )
        (1..3).each {
            ret << new MiraklValue(
                    "value$it",
                    "value${indice}_${it}Label",
                    toScalaOption(parent)
            )
        }
        ret
    }
}
