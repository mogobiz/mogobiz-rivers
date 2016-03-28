/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz.mirakl.client

import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.BulkItem
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.client.Client
import com.mogobiz.common.client.Request
import com.mogobiz.common.client.SearchResponse
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.http.client.HTTPClient
import com.mogobiz.http.client.header.HttpHeaders
import com.mogobiz.http.client.multipart.MultipartFactory
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

    static Map synchronizeCategories(RiverConfig config, List<MiraklCategory> categories){
        def buffer = new StringBuffer(String.format("\"category-code\";\"category-label\";\"logistic-class\";\"update-delete\";\"parent-code\"%n"))
        categories?.each {
            buffer.append(String.format("${it.toString()}%n"))
        }
        log.debug(buffer.toString())
        final charset = DEFAULT_CHARSET
        byte[] data = buffer.toString().getBytes(charset)
        def part = MultipartFactory.createFilePart("file", "categories.csv", data, false, "text/csv; charset=$charset", charset)
        def headers= authenticate(config)
        headers.setHeader("Accept", "application/json")
        def conn = null
        def ret = null
        try{
            conn = client.doMultipart(
                    [debug: true],
                    "${config?.clientConfig?.url}/api/categories/synchros",
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

    private static HttpHeaders authenticate(RiverConfig config){
        def headers = new HttpHeaders()
        headers.setHeader("Authorization", config?.clientConfig?.credentials?.apiKey)
        headers
    }

}
class MiraklCategory extends BulkItem{
    String label
    String logisticClass = ''

    @Override
    public String toString() {
        return "$id;$label;$logisticClass;${action == BulkAction.DELETE ? "delete" : "update"};${parent?parent.id:''}";
    }
}
