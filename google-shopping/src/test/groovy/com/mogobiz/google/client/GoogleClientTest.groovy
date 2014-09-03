package com.mogobiz.google.client

import com.mogobiz.common.client.ClientConfig
import com.mogobiz.common.client.Credentials
import com.mogobiz.common.client.Request
import com.mogobiz.common.rivers.spi.RiverConfig
import groovy.util.slurpersupport.GPathResult

/**
 * Created by stephane.manciot@ebiznext.com on 06/05/2014.
 */
class GoogleClientTest extends GroovyTestCase{

    static final String MERCHANT_ID = '100653663'
    public static final String ACCOUNT_ID = 'mogobiz@gmail.com'
    public static final String PRIVATE_KEY = 'e-z12B24'

    public void testRequestAccessToken(){
        assertNotNull GoogleClient.instance.requestAccessToken(buildClientConfig())
    }

    public void testSearch(){
        assertNotNull GoogleClient.instance.search(new Request(clientConfig: buildClientConfig()))
    }

    public void testParseEntry(){
        GPathResult result = new XmlSlurper()
                .parse(Thread.currentThread().getContextClassLoader().getResourceAsStream('feeds.xml'))
                .declareNamespace(sc: 'http://schemas.google.com/structuredcontent/2009')
        def items = []
        result.'*'.findAll{it.name() == 'entry'}.each {entry ->
            items << GoogleClient.instance.parseEntry(entry as GPathResult)
        }
        assertTrue(items.size() == 1)
    }

    private RiverConfig buildConfig() {
        new RiverConfig(
                debug: true,
                clientConfig: buildClientConfig()
        )
    }

    private ClientConfig buildClientConfig(){
        new ClientConfig(
                debug: true,
                merchant_id: MERCHANT_ID,
                credentials: new Credentials(
                        client_id: ACCOUNT_ID,
                        client_secret: PRIVATE_KEY
                )
        )
    }

}
