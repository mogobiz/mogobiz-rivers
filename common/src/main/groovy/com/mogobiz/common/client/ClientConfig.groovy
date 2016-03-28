/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.common.client

/**
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
    Map<String, Object> config
}

class Credentials{
    /**
     * api key - mirakl
     */
    String apiKey
    String client_id
    String client_secret
    String client_token
    Date expiration
    def boolean refreshToken(){
        !client_token || new Date().after(expiration)
    }
}
