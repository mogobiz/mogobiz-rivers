/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.elasticsearch.client

import com.mogobiz.common.client.BulkItemResponse
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.client.Client
import com.mogobiz.common.client.Item
import com.mogobiz.common.client.Request
import com.mogobiz.common.client.Response
import com.mogobiz.common.client.SearchResponse
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.http.client.HTTPClient
import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.BulkItem
import groovy.json.JsonBuilder
import groovy.util.logging.Log4j
import rx.Observable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import akka.dispatch.Futures

import java.util.concurrent.Callable

import static scala.collection.JavaConversions.*;

/**
 *
 */
@Log4j
final class ESClient implements Client {

    HTTPClient client = HTTPClient.instance

    private static ESClient instance

    static final enum INDEX{
        NO, ANALYZED, NOT_ANALYZED
    }

    static final enum TYPE{
        STRING, INTEGER, LONG, FLOAT, DOUBLE, BOOLEAN, NULL, DATE, OBJECT, NESTED, BINARY, ATTACHMENT
    }

    private ESClient(){}

    static ESClient getInstance(){
        if(instance == null){
            instance = new ESClient()
        }
        return instance
    }

    ESIndexResponse createIndex(
            final String url,
            final String index,
            final ESIndexSettings settings = new ESIndexSettings(),
            final Map mappings = [:],
            final Map config = [:]) {
        Map _index = [
                number_of_replicas:"${settings.number_of_replicas}",
                refresh_interval:"${settings.refresh_interval}",
                translog:[
                        disable_flush:false,
                        flush_threshold_size:"200mb",
                        flush_threshold_period:"30m",
                        interval:"5s"
                ],
                mappings:mappings
        ]
        ESIndexResponse response = new ESIndexResponse()
        JsonBuilder builder = new JsonBuilder()
        builder.call(_index)
        final String content = builder.toString()
        def conn = null
        try{
            conn = client.doPut(
                    config,
                    new StringBuffer(url).append('/').append(index?.toLowerCase()).append('/').toString(),
                    null,
                    content)
            Map _m = client.parseTextAsJSON(
                    config,
                    conn
            )
            boolean acknowledged = _m['acknowledged'] ? _m['acknowledged'] : false
            response =  new ESIndexResponse(acknowledged:acknowledged)
            if(!acknowledged && _m['error']){
                response.error = _m['error']
            }
        }
        finally{
            client.closeConnection(conn)
        }
        response
    }

    ESIndexResponse createIndex(
            final String url,
            final String index,
            final ESIndexSettings settings = new ESIndexSettings(),
            final Collection<ESMapping> mappings,
            final Map config = [:],
            final String[] languages = ['fr', 'en', 'de', 'es'],
            final String defaultLanguage = 'fr'){

        ESIndexResponse response = new ESIndexResponse()

        def _analyzis = EsAnalysis$.MODULE$.apply(asScalaSet(languages.toList().toSet()).toSet())

        Map _index = [
                number_of_replicas:"${settings.number_of_replicas}",
                refresh_interval:"${settings.refresh_interval}",
                translog:[
                    disable_flush:false,
                    flush_threshold_size:"200mb",
                    flush_threshold_period:"30m",
                    interval:"5s"
                ],
                settings: _analyzis.toJavaMap(),
                mappings:[:]
        ]

        def dynamicTemplates = []

        languages.each { language ->
            def template = [:]
            def _language = language.trim().toLowerCase()
            def match = _language + '.*'
            template << [path_match : match]
            template << [match_mapping_type : 'string']
            template << [mapping : [
                    type : 'multi_field',
                    fields:[
                            '{name}' : [
                                    type            : 'string',
                                    index           : 'analyzed',
                                    index_analyzer  : _analyzis.index_analyzer(_language).id(),
                                    search_analyzer : _analyzis.search_analyzer(_language).id(),
                                    copy_to         : 'raw',
                            ],
                            raw : [
                                    type       : 'string',
                                    index      : 'not_analyzed'
                            ]
                    ]
            ]]
            dynamicTemplates << [('template_' + _language) : template.clone()]
            template = [:]
            template << [path_match : '*.' + match]
            template << [match_mapping_type : 'string']
            template << [mapping : [
                    type : 'multi_field',
                    fields:[
                            '{name}' : [
                                type            : 'string',
                                index           : 'analyzed',
                                index_analyzer  : _analyzis.index_analyzer(_language).id(),
                                search_analyzer : _analyzis.search_analyzer(_language).id(),
                                copy_to         : 'raw',
                            ],
                            raw : [
                                    type  : 'string',
                                    index : 'not_analyzed'
                            ]
                    ]
            ]]
            dynamicTemplates << [('template_nested_' + _language) : template]
        }

        def _defaultLanguage = defaultLanguage?.trim()?.toLowerCase() ?: '*'
        def _indexAnalyzer = _analyzis.index_analyzer(_defaultLanguage).id()
        def _searchAnalyzer = _analyzis.search_analyzer(_defaultLanguage).id()

        log.info("default language for index -> $_defaultLanguage")

        mappings?.each{ mapping ->
            def m = [:]
            mapping.properties?.each { property ->
                handleMapping(m, property, _indexAnalyzer as String, _searchAnalyzer as String)
            }

            _index['mappings'][mapping.type] = [
                    _timestamp:[
                            enabled:mapping.timestamp
                    ],
                    _source:[
                            enabled:mapping.source
                    ],
                    _all:[
                            index_analyzer : _indexAnalyzer,
                            search_analyzer: _searchAnalyzer
                    ],
                    dynamic_templates : dynamicTemplates,
                    properties:m
            ]
            if(mapping.parent){
                _index['mappings'][mapping.type] << [_parent:[type:mapping.parent]]
            }
        }
        JsonBuilder builder = new JsonBuilder()
        builder.call(_index)
        final String content = builder.toString()
        def conn = null
        try{
            conn = client.doPut(
                    config,
                    new StringBuffer(url).append('/').append(index?.toLowerCase()).append('/').toString(),
                    null,
                    content)
            Map _m = client.parseTextAsJSON(
                    config,
                    conn
            )
            boolean acknowledged = _m['acknowledged'] ? _m['acknowledged'] : false
            response =  new ESIndexResponse(acknowledged:acknowledged)
            if(!acknowledged && _m['error']){
                response.error = _m['error']
            }
        }
        finally{
            client.closeConnection(conn)
        }
        response
    }

    ESIndexResponse updateIndex(final String url, final String index, final ESIndexSettings settings = new ESIndexSettings(), final Map config = [:]){
        ESIndexResponse response = new ESIndexResponse()
        def _settings = [
            index: [
                number_of_replicas:"${settings.number_of_replicas}",
                refresh_interval:settings.refresh_interval,
                translog:[
                        disable_flush:false,
                        flush_threshold_size:"200mb",
                        flush_threshold_period:"30m",
                        interval:"5s"
                ]
            ]
        ]
        JsonBuilder builder = new JsonBuilder()
        builder.call(_settings)
        final String content = builder.toString()
        def conn = null
        try{
            conn = client.doPut(config, "${url}/${index}/_settings", [:], content)
            def m = client.parseTextAsJSON(config, conn)
            def error = m.error
            if(!error){
                response.acknowledged = m.acknowledged ?: false
            }
            else{
                response.error = error
            }
        }
        finally{
            client.closeConnection(conn)
        }
        response
    }

    Map removeIndex(
            final String url,
            final String index,
            final Map config = [:]){
        def conn = null
        try{
            conn = client.doDelete(
                    config,
                    new StringBuffer(url).append('/').append(index).toString())
            return client.parseTextAsJSON(
                    config,
                    conn
            )
        }
        finally{
            client.closeConnection(conn)
        }
    }

    Set<String> retrieveAliasIndexes(final String url, final String alias, final Map config = [:]){
        def indexes = [] as Set<String>
        def conn = null
        try{
            conn = client.doGet(config, "${url}/_alias/${alias}")
            def m = client.parseTextAsJSON(config, conn)
            def error = m.error
            if(!error){
                indexes.addAll(m.keySet())
            }
        }
        finally{
            client.closeConnection(conn)
        }
        indexes
    }

    private void handleMapping(
            Map currentMap,
            final ESProperty property,
            final String index_analyzer,
            final String search_analyzer) {
        def type = property.type
        switch (type){
            case [TYPE.OBJECT, TYPE.NESTED] :
                def properties = [:]
                property.properties.each {ESProperty p ->
                    handleMapping(properties, p, index_analyzer, search_analyzer)
                }
                currentMap[property.name]  = [
                        type: type.name().toLowerCase(),
                        properties:properties
                ]
                if(property.index){
                    currentMap[property.name] << [index:property.index.name().toLowerCase()]
                }
                break
            case TYPE.STRING :
                def fieldMap = [:]
                switch(property.index){
                    case INDEX.ANALYZED:
                        fieldMap['type'] = 'multi_field'
                        fieldMap["fields"] = [
                                "${property.name}" : [
                                        type            : 'string',
                                        index           : 'analyzed',
                                        index_analyzer  : "$index_analyzer",
                                        search_analyzer : "$search_analyzer",
                                        copy_to         : 'raw'
                                ],
                                'raw' : [
                                        type  : 'string',
                                        index : 'not_analyzed'
                                ]
                        ]
                        break
                    default:
                        fieldMap["type"] = type.name().toLowerCase()
                        fieldMap["index"] = property.index.name().toLowerCase()
                        break
                }
                currentMap[property.name] = fieldMap
                break
            default :
                def fieldMap = [type: type.name().toLowerCase(), index: property.index.name().toLowerCase()]
                if (TYPE.DATE.equals(type)) {
                    fieldMap['format'] = property.format ? property.format : 'date_optional_time'
                    currentMap[property.name] = fieldMap
                } else {
                    currentMap[property.name] = fieldMap
                }
        }
        currentMap
    }

    ESAliasResponse createAlias(Map config = [:], String url, String alias, String index){
        def response = new ESAliasResponse()
        def conn = null
        try{
            conn = client.doPut(config, "${url}/$index/_alias/$alias")
            def m = client.parseTextAsJSON(config, conn)
            def error = m.error
            if(!error){
                response.acknowledged = m.acknowledged ?: false
            }
            else{
                response.error = error
            }
        }
        finally{
            client.closeConnection(conn)
        }
        response
    }

    ESAliasResponse removeAlias(Map config = [:], String url, String alias, String index){
        def response = new ESAliasResponse()
        def conn = null
        try{
            conn = client.doDelete(config, "${url}/$index/_alias/$alias")
            def m = client.parseTextAsJSON(config, conn)
            def error = m.error
            if(!error){
                response.acknowledged = m.acknowledged ?: false
            }
            else{
                response.error = error
            }
        }
        finally{
            client.closeConnection(conn)
        }
        response
    }

//    Map refreshIndex(final String url, final String index, final Map config = [:]){
//        def conn = null
//        try{
//            conn = client.doPost(
//                    config,
//                    new StringBuffer(url).append('/').append(index).append('/_refresh').toString())
//            return client.parseTextAsJSON(
//                    config,
//                    conn
//            )
//        }
//        finally{
//            client.closeConnection(conn)
//        }
//    }

    Future<BulkResponse> bulk(
            final RiverConfig config,
            final List<BulkItem> items,
            ExecutionContext ec){
        final String crlf = System.getProperty("line.separator")
        StringBuffer buffer = new StringBuffer()
        items?.each { item ->
            JsonBuilder builder = new JsonBuilder()
            def action = [
                    _index: config.clientConfig.config.index,
                    _type: item.type.toLowerCase()
            ] as Map
            def id = item.id?.trim()
            if(id && id.length() > 0){
                action << [_id:item.id]
            }
            def parent = item.parent?.id?.trim()
            if(parent && parent.length() > 0){
                action << [_parent: parent]
            }
            def map = [:]
            switch(item.action){
                case([BulkAction.INDEX, BulkAction.INSERT]):
                    map << [index:action]
                    break
                case(BulkAction.UPDATE):
                    action << ([_retry_on_conflict:3] as Map)
                    map << [update:action]
                    item.map = [doc:item.map, doc_as_upsert:true]
                    break
                default:
                    break
            }
            builder.call(map)
            buffer.append(builder.toString()).append(crlf)
            builder.call(item.map)
            buffer.append(builder.toString()).append(crlf)
        }
        final String body = buffer.toString()
        Future<BulkResponse> f = Futures.future(new Callable<BulkResponse>() {
            @Override
            BulkResponse call() throws Exception {
                def conn = null
                try{
                    def debug = config.clientConfig.debug
                    conn = client.doPost(
                            [debug:debug],
                            new StringBuffer(config.clientConfig?.url).append('/_bulk?refresh=false').toString(),
                            null,
                            body)
                    Map m = client.parseTextAsJSON(
                            [debug:debug],
                            conn
                    )
                    //boolean errors = m.containsKey('errors') ? m['errors'] as boolean : false
                    def _items = m.containsKey('items') ? m['items'] as List<Map> : []
                    new BulkResponse(
                            items:_items.collect {Map _item ->
                                def insert = _item.create as Map
                                def update = _item.update as Map
                                def delete = _item.delete as Map
                                def index = _item.index as Map
                                def map = insert ? insert : update ? update : delete ? delete : index ? index : [:]
                                def action = insert ? BulkAction.INSERT : update ? BulkAction.UPDATE : delete ?
                                        BulkAction.DELETE : index ? BulkAction.INDEX : BulkAction.UNKNOWN
                                new BulkItemResponse(
                                        id:map._id as String,
                                        type:map._type as String,
                                        action:action,
                                        status: map.status,
                                        map: map
                                )
                            },
                            responseCode: conn.responseCode
                    )
                }
                finally{
                    client.closeConnection(conn)
                }
            }
        }, ec)
        f
    }

    Observable<Future<BulkResponse>> upsert(
            final RiverConfig config,
            final List<Item> items,
            ExecutionContext ec){
        Observable.just(bulk(config, items.collect {item ->
            def id = item.id?.trim()
            new BulkItem(
                    type : item.type,
                    action: id && id.length() > 0 ? BulkAction.UPDATE : BulkAction.INSERT,
                    id: id,
                    parent: item.parent,
                    map: item.map
            )
        }, ec))
    }

    static boolean indexExists(String url, String index){
        return HTTPClient.instance.doHead([debug:true], url + '/' + index).responseCode == 200
    }

    /**
     * This method performs a search on es
     * @param request - the request to perform on es
     * @param config - a configuration map for http client
     * @return search response
     */
    ESSearchResponse search(ESRequest request, Map config = [debug: true]) {
        def hits = []
        Map aggregations = [:]
        int total = 0
        String url = request.url
        String index = request.index
        String type = request.type
        List<String> included = request.included ? request.included : []
        List<String> excluded = request.excluded ? request.excluded : []
        Map query = request.query ? request.query : [:]
        if (url && index && type) {
            def conn = null
            def StringBuffer buffer = new StringBuffer()
            buffer.append(url).append('/').append(index).append('/').append(type).append('/_search').append(request.aggregation ? "?search_type=count" : "")
            if(included.size() > 0 || excluded.size() > 0){
                query << [_source:[include:included, exclude:excluded]]
            }
            JsonBuilder builder = new JsonBuilder()
            builder.call(query)
            try {
                conn = client.doPost(
                        config,
                        buffer.toString(),
                        null,
                        builder.toString())
                def data = client.parseTextAsJSON(config, conn)
                if(data){
                    total = data['hits'] ? data['hits']['total'] as int : 0
                    if(request.aggregation){
                        aggregations = data['aggregations'] as Map
                    }
                    def results = (data['hits'] ? data['hits']['hits'] : []) as List<Map>
                    boolean multipleTypes = type?.contains(',')
                    def types = [:]
                    results.each {result ->
                        def hit
                        def highlight = result['highlight']
                        if(highlight){
                            hit = highlight
                            hit << [id:result['_id']]
                        }
                        else{
                            hit = result['_source']
                        }
                        if(multipleTypes){
                            def _type = result['_type'] as String
                            def entries = (types.get(_type) ? types.get(_type) : []) << hit
                            types.put(_type, entries)
                        }
                        else{
                            hits << hit
                        }
                    }
                    if(multipleTypes){
                        hits << types
                    }
                }
            }
            finally {
                client.closeConnection(conn)
            }
        }
        new ESSearchResponse(total: total, hits: hits, aggregations: aggregations)
    }

    SearchResponse search(Request request) {
        ESSearchResponse response = search(new ESRequest(request.properties))
        new SearchResponse(total: response.total, hits: response.hits.collect {hit -> new Item(id: hit.id, map: hit)})
    }
}

class ESMapping {
    String type
    boolean source = true
    boolean timestamp
    Collection<ESProperty> properties
    String parent
}

class ESProperty{
    //boolean source = true
    String name
    ESClient.TYPE type
    String format
    ESClient.INDEX index
    boolean multilang
    Collection<ESProperty> properties
}

class ESIndexResponse extends Response{
    boolean acknowledged
    String error
}

class ESAliasResponse extends Response{
    boolean acknowledged
    String error
}

class ESRequest extends Request{
    String url = clientConfig?.url
    String index = clientConfig?.store
    boolean aggregation = false
}

class ESSearchResponse extends Response{
    int total = 0
    List<Map> hits = []
    Map aggregations = [:]
}

class ESIndexSettings{
    int number_of_replicas = 1
    String refresh_interval = "1s"
}
