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
import com.mogobiz.mirakl.client.domain.MiraklHierarchy
import com.mogobiz.mirakl.client.domain.MiraklItem
import com.mogobiz.mirakl.client.domain.MiraklItems
import com.mogobiz.mirakl.client.io.CategoriesSynchronizationStatusResponse
import com.mogobiz.mirakl.client.io.ImportHierarchiesResponse
import com.mogobiz.mirakl.client.io.ImportResponse
import com.mogobiz.mirakl.client.io.ImportStatusResponse
import com.mogobiz.mirakl.client.io.ListValuesResponse
import com.mogobiz.mirakl.client.io.SearchShopsRequest
import com.mogobiz.mirakl.client.io.SearchShopsResponse
import com.mogobiz.mirakl.client.io.Synchronization
import com.mogobiz.mirakl.client.io.SynchronizationResponse
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
    static SynchronizationResponse synchronizeCategories(RiverConfig config, List<MiraklCategory> categories){
        def items = new MiraklItems(header: "\"category-code\";\"category-label\";\"logistic-class\";\"update-delete\";\"parent-code\"", items: categories)
        importItems(Synchronization.class, config, "/api/categories/synchros", items, "categories.csv")
    }

    /**
     * CA02 - refresh categories synchronisation status
     * @param config - river configuration
     * @param synchro - synchronization id
     * @return status of the categories synchronisation
     */
    static CategoriesSynchronizationStatusResponse refreshCategoriesSynchronizationStatus(RiverConfig config, Long synchro){
        trackStatus(CategoriesSynchronizationStatusResponse.class, config, "/api/categories/synchros", synchro)
    }

    /******************************************************************************************************************
     * Hierarchies api
     ******************************************************************************************************************/

    /**
     * H01 - Import operator hierarchies
     * @param config - river configuration
     * @param hierarchies - hierarchies to import
     * @return import response
     */
    static ImportResponse importHierarchies(RiverConfig config, List<MiraklHierarchy> hierarchies){
        def items = new MiraklItems(header: "\"hierarchy-code\";\"hierarchy-label\";\"hierarchy-parent-code\";\"update-delete\"", items: hierarchies)
        importItems(ImportHierarchiesResponse.class, config, "/api/hierarchies/imports", items, "hierarchies.csv")
    }

    /**
     * H02 - Get import information
     * @param config - river configuration
     * @param importId - tracking id
     * @return status response
     */
    static ImportStatusResponse trackHierarchiesImportStatusResponse(RiverConfig config, Long importId){
        trackStatus(ImportStatusResponse.class, config,  "/api/hierarchies/imports", importId)
    }

    /******************************************************************************************************************
     * Values api
     ******************************************************************************************************************/

    static ListValuesResponse listValues(RiverConfig config, String code = null){
        def headers= authenticate(config)
        headers.setHeader("Accept", "application/json")
        def conn = null
        def ret = null
        def params = [:]
        if(code){
            params << [code: code]
        }
        try{
            conn = client.doGet(
                    [debug: config.debug],
                    "${config?.clientConfig?.url}/api/values_lists",
                    params,
                    headers,
                    true
            )
            def responseCode = conn.responseCode
            if(responseCode != 200){
                log.error("$responseCode: ${conn.responseMessage}")
            }
            ret = new ObjectMapper().readValue(getText([debug: config.debug], conn), ListValuesResponse)
        }
        finally {
            closeConnection(conn)
        }
        ret
    }

    /******************************************************************************************************************
     * Shop api
     ******************************************************************************************************************/

    static <T extends SearchShopsResponse> T searchShops(Class<T> classz = SearchShopsResponse.class, RiverConfig config, SearchShopsRequest request){
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
            ret = new ObjectMapper().readValue(getText([debug: config.debug], conn), classz) as T
        }
        finally {
            closeConnection(conn)
        }
        ret
    }

    /******************************************************************************************************************
     * Bulk Import helper
     ******************************************************************************************************************/

    /**
     *
     * @param config - river configuration
     * @param api - api
     * @param items - items to import
     * @param fileName - file name
     * @param partName - name of the multipart
     * @param mimeType - mime type
     * @param charset - charset
     * @return T
     */
    static <T, U extends MiraklItem> T importItems(
            Class<T> classz,
            RiverConfig config,
            String api,
            MiraklItems<U> items,
            String fileName,
            String partName = "file",
            String mimeType = "text/csv",
            String charset = DEFAULT_CHARSET){
        def part = MultipartFactory.createFilePart(partName, fileName, items.getBytes(charset), false, "$mimeType; charset=$charset", charset)
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
            ret = new ObjectMapper().readValue(getText([debug: config.debug], conn), classz) as T
        }
        finally{
            closeConnection(conn)
        }
        ret
    }

    /******************************************************************************************************************
     * Tracking Status helper
     ******************************************************************************************************************/

    static <T> T trackStatus(Class<T> classz, RiverConfig config, String api, Long trackingtId){
        def headers= authenticate(config)
        headers.setHeader("Accept", "application/json")
        def conn = null
        def ret = null
        try{
            conn = client.doGet(
                    [debug: config.debug],
                    "${config?.clientConfig?.url}$api/$trackingtId",
                    [:],
                    headers,
                    true
            )
            def responseCode = conn.responseCode
            if(responseCode != 200){
                log.error("$responseCode: ${conn.responseMessage}")
            }
            ret = new ObjectMapper().readValue(getText([debug: config.debug], conn), classz) as T
        }
        finally {
            closeConnection(conn)
        }
        ret
    }

    /******************************************************************************************************************
     * Authorization helper
     ******************************************************************************************************************/

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

}

