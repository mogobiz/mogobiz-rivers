/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.google.rivers

import com.mogobiz.common.client.ClientConfig
import com.mogobiz.common.client.Credentials
import com.mogobiz.common.rivers.spi.RiverConfig

/**
 *
 */
class GoogleRiversTest extends GroovyTestCase {

    static final String MERCHANT_ID = '100653663'
    public static final String ACCOUNT_ID = '1028899612234-hgq28mq5vuf6t27fo3q81ch4ne33beo2@developer.gserviceaccount.com'
    public static final String PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\nMIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAMFA1g9gTSXoYtLN\nRFUqiKyiC0MtRxgbTllT3YBPU0XF+FcS9UnmEqSIBJ2TyRyNQKW3bFuhQg4XToZP\nXy+vnBGlZL9Sn/UOAqId/MvuUeqoFIL31mF7aGXW6ivU7kySvIVb7hySlPdi5BuH\nCIH18qDPJbgb+g4WvxQ1I6tXQoSrAgMBAAECgYEAioJp6kNfiBfzHJu6qj/+DZ1m\n7RL6rbCEi0msrFYunQezYtVXsUuReRN0G2zc6/Xhq+S3aUU/DtJtmZ4x3v/CnzG+\nkcYF8mOZvsFLYZFxg7t5hZVpiPiAYVBbpQqmR8CjhoPBiGvhgD+JLIgLjfn+aVL6\nRjuRzyXsq6BkYJEYWAECQQD8htVUXAGApQcshRCk/XMQ4Nbz/XvXkFZhJGmvE7jw\nUdUzZ59Y17E4C8AQcJMNnt7pCrXz0pOaAtyP/XWRmDmrAkEAw+lL33wsAaAgbmSk\nCvx/MYpjL7E6vKrFfOVcb/Tay9dSv46ZDoVVglJZ1+jB50dVwx5LLXORd1RrBb77\nE/HhAQJBAJrs4c5aj/VryaXvkRGYCNMPDfEsz6ClhckdPNVThT+zBNj/tswbsDcR\nRmkPl7hggqN4lb6br0BqhwSz3EBRovcCQHOCrZKykvvOXvDX9ATqRVB/aOXo2Fn/\nUnvGfE8ijpLbfVWmjUYLhZRWW9S6zwE/hSiLRwYgeudh5cw4g6+J0QECQQC5fEL5\na9BoT9kg6dumT+xJVYAYE+VpBzyxwoM4uCVCk3oiF8yedAFSBC6XCiDh3FmU3ELy\nn9VmdJdHwNklbLmp\n-----END PRIVATE KEY-----\n"

    public void testLoadRiver(){
        assertNotNull GoogleRivers.instance.loadRiver('sample')
        assertNull GoogleRivers.instance.loadRiver('pipo')
    }

    public void testExport(){
        GoogleRivers.instance.export(buildConfig())
    }

    private static RiverConfig buildConfig() {
        new RiverConfig(
                debug: true,
                clientConfig: buildClientConfig()
        )
    }

    private static ClientConfig buildClientConfig(){
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
