/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz.mirakl.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.http.client.HTTPClient
import com.mogobiz.http.client.header.HttpHeaders
import com.mogobiz.http.client.multipart.MultipartFactory
import com.mogobiz.mirakl.client.domain.MiraklAttribute
import com.mogobiz.mirakl.client.domain.MiraklCategory
import com.mogobiz.mirakl.client.domain.MiraklHierarchy
import com.mogobiz.mirakl.client.domain.MiraklItem
import com.mogobiz.mirakl.client.domain.MiraklItems
import com.mogobiz.mirakl.client.domain.MiraklOffer
import com.mogobiz.mirakl.client.domain.MiraklProduct
import com.mogobiz.mirakl.client.domain.MiraklValue
import com.mogobiz.mirakl.client.domain.OfferImportMode
import com.mogobiz.mirakl.client.io.CategoriesSynchronizationStatusResponse
import com.mogobiz.mirakl.client.io.ImportAttributesResponse
import com.mogobiz.mirakl.client.io.ImportHierarchiesResponse
import com.mogobiz.mirakl.client.io.ImportOffersResponse
import com.mogobiz.mirakl.client.io.ImportResponse
import com.mogobiz.mirakl.client.io.ImportStatusResponse
import com.mogobiz.mirakl.client.io.ImportValuesResponse
import com.mogobiz.mirakl.client.io.ListAttributesResponse
import com.mogobiz.mirakl.client.io.ListHierarchiesResponse
import com.mogobiz.mirakl.client.io.ListValuesResponse
import com.mogobiz.mirakl.client.io.OffersSynchronizationStatusResponse
import com.mogobiz.mirakl.client.io.ProductsImportStatusResponse
import com.mogobiz.mirakl.client.io.ProductsSynchronizationStatusResponse
import com.mogobiz.mirakl.client.io.SearchShopsRequest
import com.mogobiz.mirakl.client.io.SearchShopsResponse
import com.mogobiz.mirakl.client.io.Synchronization
import com.mogobiz.mirakl.client.io.SynchronizationResponse
import groovy.util.logging.Slf4j
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemOptions
import org.apache.commons.vfs2.Selectors
import org.apache.commons.vfs2.impl.StandardFileSystemManager
import org.apache.commons.vfs2.provider.sftp.IdentityInfo
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder

import static com.mogobiz.mirakl.client.domain.MiraklApi.*


import static com.mogobiz.http.client.HTTPClient.*

import static com.mogobiz.tools.ScalaTools.*

/**
 *
 */
@Slf4j
final class MiraklClient{

    private static MiraklClient instance

    private static final HTTPClient client = HTTPClient.getInstance()

    private MiraklClient(){}

    public static MiraklClient getInstance(){
        if(!instance){
            instance = new MiraklClient()
        }
        return instance
    }

    /******************************************************************************************************************
     * Categories api
     ******************************************************************************************************************/

    /**
     * CA01 - Update categories from Operator Information System (Operator Mirakl Marketplace Platform)
     * @param config - river configuration
     * @param categories - categories to update
     * @return categories synchronisation tracking id
     */
    static SynchronizationResponse synchronizeCategories(RiverConfig config, List<MiraklCategory> categories){
        def items = new MiraklItems(categoriesHeader(), toScalaList(categories), ";")
        def response = importItems(Synchronization.class, config, categoriesApi(), items, "categories.csv")
        response.setIds(categories.collect {it.uuid()})
        response
    }

    /**
     * CA02 - refresh categories synchronisation status (Operator Mirakl Marketplace Platform)
     * @param config - river configuration
     * @param importId - tracking id
     * @return categories synchronisation status
     */
    static CategoriesSynchronizationStatusResponse refreshCategoriesSynchronizationStatus(RiverConfig config, Long importId){
        trackStatus(CategoriesSynchronizationStatusResponse.class, config, categoriesApi(), importId)
    }

    /**
     * CA03 - get errors report file for categories synchronisation (Operator Mirakl Marketplace Platform)
     * @param config - river configuration
     * @param importId - tracking id
     * @return categories synchronization error report
     */
    static String loadCategoriesSynchronizationErrorReport(RiverConfig config, Long importId){
        loadSynchronizationErrorReport(config, categoriesApi(), importId)
    }

    /******************************************************************************************************************
     * Hierarchies api
     ******************************************************************************************************************/

    /**
     * H11 - List hierarchies related (parents and children) to the given hierarchy (Front Mirakl Catalog Integration)
     * @param config - config
     * @param hierarchy - [optional] The code of the hierarchy
     * @param max_level -  [optional] Number of children hierarchy levels to retrieve. If not specified, all child hierarchies are retrieved
     * @return The list of hierarchies
     */
    static ListHierarchiesResponse listHierarchies(RiverConfig config, String hierarchy = null, int max_level = -1){
        def headers= authenticate(config?.clientConfig?.credentials?.apiKey)
        headers.setHeader("Accept", "application/json")
        def conn = null
        def ret = null
        def params = [:]
        if(hierarchy){
            params << [hierarchy: hierarchy]
        }
        if(max_level >= 0){
            params << [max_level: max_level]
        }
        try{
            conn = client.doGet(
                    [debug: config.debug],
                    "${config?.clientConfig?.merchant_url}/api/hierarchies",
                    params,
                    headers,
                    true
            )
            def responseCode = conn.responseCode
            if(responseCode != 200){
                log.error("$responseCode: ${conn.responseMessage}")
            }
            ret = new ObjectMapper().readValue(getText([debug: config.debug], conn), ListHierarchiesResponse)
        }
        finally {
            closeConnection(conn)
        }
        ret
    }

    /**
     * H01 - Import operator hierarchies (Front Mirakl Catalog Integration)
     * @param config - river configuration
     * @param hierarchies - hierarchies to import
     * @return hierarchies synchronisation tracking id
     */
    static ImportResponse importHierarchies(RiverConfig config, List<MiraklHierarchy> hierarchies){
        def items = new MiraklItems(hierarchiesHeader(), toScalaList(hierarchies), ";")
        def response = importItems(ImportHierarchiesResponse.class, config, hierarchiesApi(), items, "hierarchies.csv")
        response.setIds(hierarchies.collect {it.code})
        response
    }

    /**
     * H02 - refresh hierarchies synchronisation status (Front Mirakl Catalog Integration)
     * @param config - river configuration
     * @param importId - tracking id
     * @return hierarchies synchronisation status
     */
    static ImportStatusResponse trackHierarchiesImportStatusResponse(RiverConfig config, Long importId){
        trackStatus(ImportStatusResponse.class, config,  hierarchiesApi(), importId)
    }

    /**
     * H03 - get errors report file for hierarchies synchronisation (Front Mirakl Catalog Integration)
     * @param config - river configuration
     * @param importId - tracking id
     * @return hierarchies synchronisation error report
     */
    static String loadHierarchiesSynchronizationErrorReport(RiverConfig config, Long importId){
        loadSynchronizationErrorReport(config, hierarchiesApi(), importId)
    }

    /******************************************************************************************************************
     * Values api
     ******************************************************************************************************************/

    /**
     * VL11 - Get information about operator's values lists (Front Mirakl Catalog Integration)
     * @param config - config
     * @param code - [optional] The operator's values list code. If not specified, all values lists are retrieved
     * @return Operator Values Lists
     */
    static ListValuesResponse listValues(RiverConfig config, String code = null){
        def headers= authenticate(config?.clientConfig?.credentials?.apiKey)
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
                    "${config?.clientConfig?.merchant_url}/api/values_lists",
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

    /**
     * VL01 - Send a file to create, update or delete values list (Front Mirakl Catalog Integration)
     * @param config - config
     * @param values - values
     * @return values synchronization tracking id
     */
    static ImportResponse importValues(RiverConfig config, List<MiraklValue> values = []){
        def itemsCollection = values.collect { item ->
            item.action = BulkAction.UPDATE
            "${item.parent.get().code}${item.code}"
        }
        listValues(config).valuesLists.each{valuesList ->
            valuesList.values.findAll {value ->
                final str = "${valuesList.code}${value.code}"
                !(str in itemsCollection)
            }.each {value ->
                def parent = new MiraklValue(valuesList.code, valuesList.label, BulkAction.UNKNOWN, toScalaOption(null))
                values << new MiraklValue(
                        value.code,
                        value.label,
                        BulkAction.DELETE,
                        toScalaOption(parent)
                )
            }
        }
        def items = new MiraklItems<MiraklValue>(valuesHeader(), toScalaList(values), ";")
        def response = importItems(ImportValuesResponse.class, config, valuesApi(), items, "values_lists.csv")
        response.setIds(values.collect {it.code})
        response
    }

    /**
     * VL02 - refresh values synchronisation status (Front Mirakl Catalog Integration)
     * @param config - river configuration
     * @param importId - tracking id
     * @return values synchronization status
     */
    static ImportStatusResponse trackValuesImportStatusResponse(RiverConfig config, Long importId){
        trackStatus(ImportStatusResponse.class, config,  valuesApi(), importId)
    }

    /**
     * VL03 - get errors report file for values synchronisation (Front Mirakl Catalog Integration)
     * @param config - river configuration
     * @param importId - tracking id
     * @return values synchronization error report
     */
    static String loadValuesSynchronizationErrorReport(RiverConfig config, Long importId){
        loadSynchronizationErrorReport(config, valuesApi(), importId)
    }

    /******************************************************************************************************************
     * Product Attributes api
     ******************************************************************************************************************/

    /**
     * PM11 - Get attributes configuration (Front Mirakl Catalog Integration)
     * @param config - river configuration
     * @param hierarchy - Code of the hierarchy
     * @param max_level - Number of children hierarchy levels to retrieve
     * @return List of attributes configuration
     */
    static ListAttributesResponse listAttributes(RiverConfig config, String hierarchy = null, Long max_level = null){
        def headers= authenticate(config?.clientConfig?.credentials?.apiKey)
        headers.setHeader("Accept", "application/json")
        def conn = null
        def ret = null
        def params = [:]
        if(hierarchy){
            params << [hierarchy: hierarchy]
        }
        if(max_level){
            params << [max_level: max_level]
        }
        try{
            conn = client.doGet(
                    [debug: config.debug],
                    "${config?.clientConfig?.merchant_url}/api/products/attributes",
                    params,
                    headers,
                    true
            )
            def responseCode = conn.responseCode
            if(responseCode != 200){
                log.error("$responseCode: ${conn.responseMessage}")
            }
            ret = new ObjectMapper().readValue(getText([debug: config.debug], conn), ListAttributesResponse)
        }
        finally {
            closeConnection(conn)
        }
        ret
    }

    /**
     * PM01 - Import operator attributes (Front Mirakl Catalog Integration)
     * @param config - river config
     * @param attributes - attributes
     * @return attributes synchronization tracking id
     */
    static ImportResponse importAttributes(RiverConfig config, List<MiraklAttribute> attributes = []){
        def items = new MiraklItems(attributesHeader(), toScalaList(attributes), ";")
        def response = importItems(ImportAttributesResponse.class, config, attributesApi(), items, "attributes.csv")
        response.setIds(attributes.collect {it.code})
        response
    }

    /**
     * PM02 - refresh attributes synchronisation status (Front Mirakl Catalog Integration)
     * @param config - river config
     * @param importId - the identifier of the import
     * @return attributes synchronisation status
     */
    static ImportStatusResponse trackAttributesImportStatusResponse(RiverConfig config, Long importId){
        trackStatus(ImportStatusResponse.class, config,  attributesApi(), importId)
    }

    /**
     * PM03 - get errors report file for attributes synchronisation (Front Mirakl Catalog Integration)
     * @param config - river configuration
     * @param importId - tracking id
     * @return attributes synchronization error report
     */
    static String loadAttributesSynchronizationErrorReport(RiverConfig config, Long importId){
        loadSynchronizationErrorReport(config, attributesApi(), importId)
    }

    /******************************************************************************************************************
     * Products api
     ******************************************************************************************************************/
    /**
     * P21 - Update products from Operator Information System  (Operator Mirakl Marketplace Platform)
     * @param config - river config
     * @param products - products
     * @return attributes synchronization tracking id
     */
    static SynchronizationResponse synchronizeProducts(RiverConfig config, List<MiraklProduct> products){
        def items = new MiraklItems(productsHeader(), toScalaList(products), ";")
        def response = importItems(Synchronization.class, config, productsApi(), items, "products.csv")
        response.setIds(products?.collect {it.code()})
        response
    }

    /**
     * P22 - refresh products synchronisation status (Operator Mirakl Marketplace Platform)
     * @param config - river configuration
     * @param importId - tracking id
     * @return products synchronisation status
     */
    static ProductsSynchronizationStatusResponse refreshProductsSynchronizationStatus(RiverConfig config, Long importId){
        trackStatus(ProductsSynchronizationStatusResponse.class, config, productsApi(), importId)
    }

    /**
     * P23 - get errors report file for categories synchronisation (Operator Mirakl Marketplace Platform)
     * @param config - river configuration
     * @param importId - tracking id
     * @return categories synchronization error report
     */
    static String loadProductsSynchronizationErrorReport(RiverConfig config, Long importId){
        loadSynchronizationErrorReport(config, productsApi(), importId)
    }

    /**
     * P42 - refresh products import synchronisation status (Front Mirakl Catalog Integration)
     * @param config - river configuration
     * @param importId - tracking id
     * @return products synchronisation status
     */
    static ProductsImportStatusResponse trackProductsImportStatusResponse(RiverConfig config, Long importId){
        trackStatus(ProductsImportStatusResponse.class, config, importProductsApi(), importId, config?.clientConfig?.credentials?.apiKey)
    }

    /**
     * P44 - get errors report file for products import synchronisation (Front Mirakl Catalog Integration)
     * @param config - river configuration
     * @param importId - tracking id
     * @return products import synchronization error report
     */
    static String loadProductsImportSynchronizationErrorReport(RiverConfig config, Long importId){
        loadSynchronizationErrorReport(config, importProductsApi(), importId, config?.clientConfig?.credentials?.apiKey)
    }

    /**
     * P47 - get transformation errors report file for products import synchronisation (Front Mirakl Catalog Integration)
     * @param config - river configuration
     * @param importId - tracking id
     * @return products import transformation error report
     */
    static String loadProductsImportTransformationErrorReport(RiverConfig config, Long importId){
        loadSynchronizationErrorReport(config, importProductsApi(), importId, config?.clientConfig?.credentials?.apiKey, "transformation_error_report")
    }

    /******************************************************************************************************************
     * Offers api
     ******************************************************************************************************************/
    /**
     * 0F01 - Import offers (Operator Mirakl Marketplace Platform)
     * @param config - river config
     * @param offers - offers
     * @return offers synchronization tracking id
     */
    static ImportOffersResponse importOffers(RiverConfig config, List<MiraklOffer> offers = [], List<MiraklProduct> products = [], OfferImportMode mode = OfferImportMode.NORMAL){
        List<MiraklOffer> list = []
        list.addAll(offers)
        list = list.unique { a, b -> a.code() <=> b.code()}

        if(!products || products.size() == 0){
            products = offers.findAll {it.product().isDefined()}.collect{it.product().get()}.unique { a, b -> a.code() <=> b.code() }
        }

        // import products and offers using OF01
        def items = new MiraklItems(config.clientConfig.config.offersHeader as String, toScalaList(list), ";")
        def params = [:]
        params << [shop: config?.clientConfig?.merchant_id]
        params << [import_mode: mode.toString()]
        params << [with_products: "true"]
        final body = items.toString()
        if(config.debug && log.isDebugEnabled()){
            log.debug(body)
        }
        def response = importItems(ImportOffersResponse.class, config, offersApi(), body.getBytes(DEFAULT_CHARSET), "offers.csv", config.clientConfig.credentials.apiKey, params)

        // TODO products synchronization should be handled through sftp synchronization
        if(config?.clientConfig?.credentials?.frontKey){
            // synchronize products using P21
            Long productSynchroId = null
            if(products?.size() > 0){
                productSynchroId = synchronizeProducts(config, products).synchroId
            }
            response.setProductSynchroId(productSynchroId)
        }

        response.setIds(list?.collect {it.code()})
        response.setProductIds(products?.collect {it.code()})

        response
    }

    /**
     * OF02 - refresh attributes synchronisation status (Operator Mirakl Marketplace Platform)
     * @param config - river config
     * @param importId - the identifier of the import
     * @return offers synchronisation status
     */
    static OffersSynchronizationStatusResponse trackOffersImportStatusResponse(RiverConfig config, Long importId){
        trackStatus(OffersSynchronizationStatusResponse.class, config,  offersApi(), importId, config?.clientConfig?.credentials?.apiKey)
    }

    /**
     * OF03 - get errors report file for offers synchronisation (Operator Mirakl Marketplace Platform)
     * @param config - river configuration
     * @param importId - tracking id
     * @return offers synchronization error report
     */
    static String loadOffersSynchronizationErrorReport(RiverConfig config, Long importId){
        loadSynchronizationErrorReport(config, offersApi(), importId, config?.clientConfig?.credentials?.apiKey)
    }


    /******************************************************************************************************************
     * Shop api
     ******************************************************************************************************************/

    static <T extends SearchShopsResponse> T searchShops(Class<T> classz = SearchShopsResponse.class, RiverConfig config, SearchShopsRequest request){
        def headers= authenticate(config?.clientConfig?.credentials?.frontKey)
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
                    "${config?.clientConfig?.merchant_url}/api/shops",
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
     * Miscellaneous apis
     ******************************************************************************************************************/

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
            String key = null,
            Map<String, String> params = [:],
            String partName = "file",
            String mimeType = "text/csv",
            String charset = DEFAULT_CHARSET){
        importItems(classz, config, api, items.toString().getBytes(charset), fileName, key, params, partName, mimeType, charset)
    }

    static <T> T importItems(
            Class<T> classz,
            RiverConfig config,
            String api,
            byte[] bytes,
            String fileName,
            String key = null,
            Map<String, String> params = [:],
            String partName = "file",
            String mimeType = "text/csv",
            String charset = DEFAULT_CHARSET){
        def parts = []
        parts << MultipartFactory.createFilePart(partName, fileName, bytes, false, "$mimeType; charset=$charset", charset)
        params?.each {k,v ->
            parts << MultipartFactory.createParamPart(k, v)
        }
        def headers= key ? authenticate(key) : authenticate(config?.clientConfig?.credentials?.frontKey)
        headers.setHeader("Accept", "application/json")
        def conn = null
        def ret = null
        final String url = "${config?.clientConfig?.merchant_url}$api"
//        final String u = params ? addParams(url.indexOf('?') < 0 ? url + '?' : url, params, charset).toString() : url
        try{
            conn = client.doMultipart(
                    [debug: config.debug],
                    url,
                    parts,
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

    static <T> T trackStatus(Class<T> classz, RiverConfig config, String api, Long trackingId, String key = null){
        def headers= key ? authenticate(key) : authenticate(config?.clientConfig?.credentials?.frontKey)
        headers.setHeader("Accept", "application/json")
        def conn = null
        def ret = null
        try{
            conn = client.doGet(
                    [debug: config.debug],
                    "${config?.clientConfig?.merchant_url}$api/$trackingId",
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

    static String loadSynchronizationErrorReport(RiverConfig config, String api, Long synchro, String key = null, String report = "error_report"){
        def headers= key ? authenticate(key) : authenticate(config?.clientConfig?.credentials?.frontKey)
        headers.setHeader("Accept", "application/json")
        def conn = null
        def ret = null
        try{
            conn = client.doGet(
                    [debug: config.debug],
                    "${config?.clientConfig?.merchant_url}$api/$synchro/$report",
                    [:],
                    headers,
                    true
            )
            def responseCode = conn.responseCode
            if(responseCode != 200){
                log.error("$responseCode: ${conn.responseMessage}")
            }
            ret = getText([debug: config.debug], conn)
        }
        finally {
            closeConnection(conn)
        }
        ret
    }

    static synchronizeImports(MiraklSftpConfig config){
        def sysManager = new StandardFileSystemManager()

        //create options for sftp
        FileSystemOptions options = new FileSystemOptions();
        //ssh key
        SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(options, "no");
        //set root directory to user home
        SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(options, false);
        //timeout
        SftpFileSystemConfigBuilder.getInstance().setTimeout(options, 10000);

        if (config.keyPath) {
            SftpFileSystemConfigBuilder.getInstance().setIdentityInfo(options, new IdentityInfo(new File(config.keyPath), config.passphrase?.bytes));
        }

        final remoteURL = "sftp://$config.username${config.keyPath ? ":$config.password" : ""}@$config.remoteHost$config.remotePath"

        try {
            sysManager.init()

            FileObject localFile = sysManager.resolveFile(config.localPath)

            FileObject remoteFile = sysManager.resolveFile(remoteURL, options)

            //Selectors.SELECT_FILES --> A FileSelector that selects only the base file/folder.
            localFile.copyFrom(remoteFile, Selectors.SELECT_FILES)


        } catch (Exception e) {
            log.error(e.message, e)
            return false
        }finally{
            sysManager.close()
        }
        return true
    }

    /******************************************************************************************************************
     * Authorization helper
     ******************************************************************************************************************/

    /**
     * add headers to authenticate Operator
     * @param key - api key to use
     * @return http headers with authorization
     */
    private static HttpHeaders authenticate(String key){
        def headers = new HttpHeaders()
        headers.setHeader("Authorization", key)
        headers
    }

    static class MiraklSftpConfig{
        String remoteHost = "127.0.0.1"
        String remotePath = "/home/mirakl/imports"
        String localPath = "/tmp"
        String username = "mirakl"
        String password = ""
        String keyPath = "/home/mirakl/.ssh/id_dsa"
        String passphrase = null
    }
}

