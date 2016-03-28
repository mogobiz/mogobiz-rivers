/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz.mirakl.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.mogobiz.common.client.BulkItem
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.client.Client
import com.mogobiz.common.client.Request
import com.mogobiz.common.client.SearchResponse
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.http.client.HTTPClient
import com.mogobiz.http.client.header.HttpHeaders
import com.mogobiz.http.client.multipart.MultipartFactory
import com.mogobiz.mirakl.client.domain.MiraklCategory
import com.mogobiz.mirakl.client.domain.SearchShopResponse
import groovy.util.logging.Slf4j
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import static com.mogobiz.http.client.HTTPClient.*
import static com.mogobiz.mirakl.client.domain.Shop.*

/**
 *
 */
@Slf4j
final class MiraklClient implements Client{

    private static MiraklClient instance

    private static final HTTPClient client = HTTPClient.getInstance()

    private MiraklClient(){}

    public static MiraklClient getInstance(){
        if(!instance){
            instance = new MiraklClient()
        }
        return instance
    }

    @Override
    Future<BulkResponse> bulk(RiverConfig config, List<BulkItem> items, ExecutionContext ec) {
        return null
    }

    @Override
    SearchResponse search(Request request) {
        return null
    }

    /******************************************************************************************************************
     * Categories api
     ******************************************************************************************************************/

    /**
     * CA01 - Update categories from Operator Information System
     * @param config - river configuration
     * @param categories - categories to update
     * @return synchronization id
     */
    static Map synchronizeCategories(RiverConfig config, List<MiraklCategory> categories){
        def buffer = new StringBuffer(String.format("\"category-code\";\"category-label\";\"logistic-class\";\"update-delete\";\"parent-code\"%n"))
        categories?.each {
            buffer.append(String.format("${it.toString()}%n"))
        }
        log.debug(buffer.toString())
        final charset = DEFAULT_CHARSET
        byte[] data = buffer.toString().getBytes(charset)
        synchronize(config, "/api/categories/synchros", "categories.csv", data)
    }

    /**
     * CA02 - Get status of the categories synchronisation
     * @param config - river configuration
     * @param synchro - synchronization id
     * @return status of the categories synchronisation
     */
    static Map refreshCategoriesSynchronizationStatus(RiverConfig config, Integer synchro){
        refreshSynchronizationStatus(config, "/api/categories/synchros", synchro)
    }

    /**
     * add headers to authenticate Operator
     * @param config - river configuration
     * @return http headers with authorization
     */
    private static HttpHeaders authenticate(RiverConfig config){
        def headers = new HttpHeaders()
        headers.setHeader("Authorization", config?.clientConfig?.credentials?.apiKey)
        headers
    }

    /******************************************************************************************************************
     * Shop api
     ******************************************************************************************************************/

    static SearchShopResponse searchShops(RiverConfig config, SearchShopRequest request){
        def headers= authenticate(config)
        headers.setHeader("Accept", "application/json")
        def conn = null
        def ret = null
        try{
            conn = client.doGet(
                    [debug: config.debug],
                    "${config?.clientConfig?.url}/api/shops",
                    [:],
                    headers,
                    true
            )
            def responseCode = conn.responseCode
            if(responseCode != 200){
                log.error("$responseCode: ${conn.responseMessage}")
            }
            def text = getText([debug: config.debug], conn)
            def objectMapper = new ObjectMapper()
            ret = objectMapper.readValue(text, SearchShopResponse.class)
        }
        finally {
            closeConnection(conn)
        }
        ret
    }

    /******************************************************************************************************************
     * Synchronization api
     ******************************************************************************************************************/

    /**
     * Synchronization from Operator Information System
     * @param config - river configuration
     * @param api - api
     * @param fileName - file name
     * @param data - multipart data
     * @param partName - name of the multipart
     * @param mimeType - mime type
     * @param charset - charset
     * @return synchronization id
     */
    private static Map synchronize(
            RiverConfig config,
            String api,
            String fileName,
            byte[] data,
            String partName = "file",
            String mimeType = "text/csv",
            String charset = DEFAULT_CHARSET){
        def part = MultipartFactory.createFilePart(partName, fileName, data, false, "$mimeType; charset=$charset", charset)
        def headers= authenticate(config)
        headers.setHeader("Accept", "application/json")
        def conn = null
        def ret = null
        try{
            conn = client.doMultipart(
                    [debug: config.debug],
                    "${config?.clientConfig?.url}$api",
                    [part],
                    headers,
                    true
            )
            def responseCode = conn.responseCode
            if(responseCode != 201){
                log.error("$responseCode: ${conn.responseMessage}")
            }
            ret = parseTextAsJSON(conn)
        }
        finally{
            closeConnection(conn)
        }
        ret
    }

    /**
     * Get status of the synchronisation
     * @param config - river configuration
     * @param api - api
     * @param synchro - synchronization id
     * @return status of the synchronisation
     */
    private static Map refreshSynchronizationStatus(RiverConfig config, String api, Integer synchro){
        def headers= authenticate(config)
        headers.setHeader("Accept", "application/json")
        def conn = null
        def ret = null
        try{
            conn = client.doGet(
                    [debug: config.debug],
                    "${config?.clientConfig?.url}$api/$synchro",
                    [:],
                    headers,
                    true
            )
            def responseCode = conn.responseCode
            if(responseCode != 200){
                log.error("$responseCode: ${conn.responseMessage}")
            }
            ret = parseTextAsJSON(conn)
        }
        finally {
            closeConnection(conn)
        }
        ret
    }
}

class SearchShopRequest{
    List<String> shop_ids = []
    PremiumState premium = PremiumState.DEFAULT
    ShopState state
    Date updated_since
    boolean paginate = true
}

