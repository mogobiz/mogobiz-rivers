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
import com.mogobiz.mirakl.client.io.CategoriesSynchronizationStatusResponse
import com.mogobiz.mirakl.client.io.SearchShopsRequest
import com.mogobiz.mirakl.client.io.SearchShopsResponse
import com.mogobiz.mirakl.client.io.Synchronization
import com.mogobiz.mirakl.client.io.SynchronizationStatusResponse
import groovy.util.logging.Slf4j
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import static com.mogobiz.http.client.HTTPClient.*

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
     * @return synchronization
     */
    static Synchronization synchronizeCategories(RiverConfig config, List<MiraklCategory> categories){
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
     * CA02 - refresh categories synchronisation status
     * @param config - river configuration
     * @param synchro - synchronization id
     * @return status of the categories synchronisation
     */
    static CategoriesSynchronizationStatusResponse refreshCategoriesSynchronizationStatus(RiverConfig config, Long synchro){
        refreshSynchronizationStatus(
                config,
                "/api/categories/synchros",
                new CategoriesSynchronizationStatusResponse(synchroId: synchro)
        )
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

    static SearchShopsResponse searchShops(RiverConfig config, SearchShopsRequest request){
        def headers= authenticate(config)
        headers.setHeader("Accept", "application/json")
        def conn = null
        def ret = null
        try{
            def params = [:]
            params << [shop_ids: request?.shopIds?.join(",")]
            params << [premium: request?.premium?.toString()?:SearchShopsRequest.Premium.ALL.toString()]
            params << [state: request?.state?.toString()]
            params << [updated_since: request?.updatedSince?.toString()]
            params << [paginate: request?.paginate]
            conn = client.doGet(
                    [debug: config.debug],
                    "${config?.clientConfig?.url}/api/shops",
                    params,
                    headers,
                    true
            )
            def responseCode = conn.responseCode
            if(responseCode != 200){
                log.error("$responseCode: ${conn.responseMessage}")
            }
            def text = getText([debug: config.debug], conn)
            def objectMapper = new ObjectMapper()
            ret = objectMapper.readValue(text, SearchShopsResponse.class)
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
    private static Synchronization synchronize(
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
            def text = getText([debug: config.debug], conn)
            def objectMapper = new ObjectMapper()
            ret = objectMapper.readValue(text, Synchronization.class)
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
    private static <T extends SynchronizationStatusResponse> T refreshSynchronizationStatus(RiverConfig config, String api, T synchro){
        def headers= authenticate(config)
        headers.setHeader("Accept", "application/json")
        def conn = null
        def ret = null
        try{
            conn = client.doGet(
                    [debug: config.debug],
                    "${config?.clientConfig?.url}$api/${synchro.synchroId}",
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
            ret = objectMapper.readValue(text, synchro.getClass())
        }
        finally {
            closeConnection(conn)
        }
        ret as T
    }
}

