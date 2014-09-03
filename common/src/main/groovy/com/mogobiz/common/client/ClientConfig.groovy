package com.mogobiz.common.client

/**
 * Created by smanciot on 16/05/2014.
 */
class ClientConfig {
    /**
     * id store
     */
    String store
    boolean debug = true
    String url
    /**
     * merchant id - google shopping
     */
    String merchant_id
    /**
     * merchant url - google shopping
     */
    String merchant_url
    Credentials credentials
    Map config
}

class Credentials{
    String client_id
    String client_secret
    String client_token
}