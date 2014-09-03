package com.mogobiz.google.rivers

import com.mogobiz.common.client.ClientConfig
import com.mogobiz.common.client.Credentials
import com.mogobiz.common.rivers.spi.RiverConfig

/**
 * Created by smanciot on 16/05/2014.
 */
class GoogleRiversTest extends GroovyTestCase {

    static final String MERCHANT_ID = '100653663'
    public static final String ACCOUNT_ID = 'mogobiz@gmail.com'
    public static final String PRIVATE_KEY = 'e-z12B24'

    public void testLoadRiver(){
        assertNotNull GoogleRivers.instance.loadRiver('sample')
        assertNull GoogleRivers.instance.loadRiver('pipo')
    }

    public void testExport(){
        GoogleRivers.instance.export(buildConfig())
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
