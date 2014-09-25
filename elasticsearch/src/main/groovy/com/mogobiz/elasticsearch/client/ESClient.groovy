package com.mogobiz.elasticsearch.client

import akka.dispatch.Mapper
import com.mogobiz.common.client.BulkItemResponse
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.client.Client
import com.mogobiz.common.client.ClientConfig
import com.mogobiz.common.client.Item
import com.mogobiz.common.client.ItemResponse
import com.mogobiz.common.client.Request
import com.mogobiz.common.client.Response
import com.mogobiz.common.client.SearchResponse
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.http.client.HTTPClient
import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.BulkItem
import groovy.json.JsonBuilder
import rx.Observable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import akka.dispatch.Futures

import java.util.concurrent.Callable

/**
 * Created by stephane.manciot@ebiznext.com on 02/02/2014.
 */
final class ESClient implements Client {

    HTTPClient client = HTTPClient.instance

    private static ESClient instance

    static final enum INDEX{
        NO, ANALYZED, NOT_ANALYZED
    }

    static final enum TYPE{
        STRING, INTEGER, LONG, FLOAT, DOUBLE, BOOLEAN, NULL, DATE, OBJECT, NESTED
    }

    static final enum FILTER_TYPE{
        STOP,STEM,NGRAM
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
            final Collection<ESMapping> mappings,
            final Map config = [:],
            final String[] languages = ['fr', 'en', 'de', 'es'],
            final String defaultLanguage = 'fr',
            final ESAnalysis analysis = defaultAnalysis()){
        boolean acknowledged = false

        ESIndexResponse response = new ESIndexResponse()

        def _analyzis = [:]

        def filters = [:]
        analysis.filters?.each {filter ->
            switch (filter.type){
                case(FILTER_TYPE.STOP):
                    filters[filter.id] = [type: 'stop', stopwords:filter.stopwords]
                    break
                case(FILTER_TYPE.STEM):
                    filters[filter.id] = [type: 'stemmer', name:filter.name]
                    break
                case(FILTER_TYPE.NGRAM):
                    filters[filter.id] = [type: 'nGram'] << filter.options
                    break
                default:
                    break
            }
        }
        if(!filters.isEmpty()){
            _analyzis << [filter:filters]
        }

        def analyzers = [:]

        def _index_analyzers = [:]
        analysis.index_analyzers?.each {analyzer ->
            if(languages.contains(analyzer.lang) || '*'.equals(analyzer.lang)){
                analyzers[analyzer.id] = [
                            type:analyzer.type,
                            tokenizer:analyzer.tokenizer,
                            filter:analyzer.filters,
                            char_filter:analyzer.charFilters
                ]
                _index_analyzers[analyzer.lang] = analyzer.id
            }
        }

        def _search_analyzers = [:]
        analysis.search_analyzers?.each {analyzer ->
            if(languages.contains(analyzer.lang) || '*'.equals(analyzer.lang)){
                analyzers[analyzer.id] = [
                        type:analyzer.type,
                        tokenizer:analyzer.tokenizer,
                        filter:analyzer.filters,
                        char_filter:analyzer.charFilters
                ]
                _search_analyzers[analyzer.lang] = analyzer.id
            }
        }

        if(!analyzers.isEmpty()){
            _analyzis << [analyzer:analyzers]
        }

        def tokenizers = []
        analysis.tokenizers?.each {tokenizer ->
            tokenizers[tokenizer.id] = [type:tokenizer.type, mode:tokenizer.mode]
        }
        if(!tokenizers.isEmpty()){
            _analyzis << [tokenizer:tokenizers]
        }

        Map _index = [
                number_of_replicas:"${settings.number_of_replicas}",
                refresh_interval:"${settings.refresh_interval}",
                translog:[
                    disable_flush:false,
                    flush_threshold_size:"200mb",
                    flush_threshold_period:"30m",
                    interval:"5s"
                ],
                settings:
                [
                    analysis:_analyzis
                ],
                mappings:[:]
        ]

        def dynamicTemplates = []

        languages.each {language ->
            def template = [:]
            def match = language + '.*'
            template << [path_match : match]
            template << [match_mapping_type : 'string']
            template << [mapping : [
                    type : 'multi_field',
                    fields:[
                            '{name}' : [
                                    type            : 'string',
                                    index           : 'analyzed',
                                    index_analyzer  : _index_analyzers[language],
                                    search_analyzer : _search_analyzers[language],
                                    copy_to         : 'raw',
                            ],
                            raw : [
                                    type       : 'string',
                                    index      : 'not_analyzed'
                            ]
                    ]
            ]]
            dynamicTemplates << [('template_' + language) : template.clone()]
            template = [:]
            template << [path_match : '*.' + match]
            template << [match_mapping_type : 'string']
            template << [mapping : [
                    type : 'multi_field',
                    fields:[
                            '{name}' : [
                                type            : 'string',
                                index           : 'analyzed',
                                index_analyzer  : _index_analyzers[language],
                                search_analyzer : _search_analyzers[language],
                                copy_to         : 'raw',
                            ],
                            raw : [
                                    type  : 'string',
                                    index : 'not_analyzed'
                            ]
                    ]
            ]]
            dynamicTemplates << [('template_nested_' + language) : template]
        }

        mappings?.each{ mapping ->
            def m = [:]
            mapping.properties?.each { property ->
                handleMapping(m, property, languages, defaultLanguage, _index_analyzers, _search_analyzers)
            }

            _index['mappings'][mapping.type] = [
                    _timestamp:[
                            enabled:mapping.timestamp
                    ],
                    _source:[
                            enabled:mapping.source
                    ],
                    _all:[
                            index_analyzer : defaultLanguage ? _index_analyzers[defaultLanguage] : _index_analyzers['*'],
                            search_analyzer: defaultLanguage ? _search_analyzers[defaultLanguage] : _search_analyzers['*']
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
            acknowledged = _m['acknowledged'] ? _m['acknowledged'] : false
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
        def conn
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
        def conn
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
            final String[] languages,
            final String defaultLanguage,
            final Map index_analyzers,
            final Map search_analyzers) {
        def type = property.type
        switch (type){
            case [TYPE.OBJECT, TYPE.NESTED] :
                def properties = [:]
                property.properties.each {ESProperty p ->
                    handleMapping(properties, p, languages, defaultLanguage, index_analyzers, search_analyzers)
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
                                        type    : 'string',
                                        index   : 'analyzed',
                                        copy_to : 'raw',
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

    Observable<Future<BulkResponse>> bulk(
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
            ]
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
                    action << [refresh:false]
                    map << [index:action]
                    break
                case(BulkAction.UPDATE):
                    action << [refresh:false, _retry_on_conflict:3]
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
                            new StringBuffer(config.clientConfig?.url).append('/_bulk').toString(),
                            null,
                            body)
                    Map m = client.parseTextAsJSON(
                            [debug:debug],
                            conn
                    )
                    boolean errors = m.containsKey('errors') ? m['errors'] as boolean : false
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
        Observable.from(f)
    }

    Observable<Future<BulkResponse>> upsert(
            final RiverConfig config,
            final List<Item> items,
            ExecutionContext ec){
        bulk(config, items.collect {item ->
            def id = item.id?.trim()
            new BulkItem(
                    type : item.type,
                    action: id && id.length() > 0 ? BulkAction.UPDATE : BulkAction.INSERT,
                    id: id,
                    parent: item.parent,
                    map: item.map
            )
        }, ec)
    }

    static boolean indexExists(String url, String index){
        return HTTPClient.instance.doHead([debug:true], url + '/' + index).responseCode == 200
    }

    ESAnalysis defaultAnalysis(){
        ESAnalysis analysis = new ESAnalysis(
                filters : [],
                index_analyzers: [],
                search_analyzers: [],
                tokenizers: []
        )

        // nGram
        analysis.filters << new ESFilter(
                id: 'nGram_filter',
                type:FILTER_TYPE.NGRAM,
                options:[min_gram:2, max_gram:20]
        )

        analysis.index_analyzers << new ESAnalyzer(
                id: 'default_index_analyzer',
                type: 'custom',
                lang: '*',
                tokenizer: 'icu_tokenizer',
                filters: ["icu_folding", "icu_normalizer", "nGram_filter"],
                charFilters: ["html_strip"]
        )
        analysis.search_analyzers << new ESAnalyzer(
                id: 'default_search_analyzer',
                type: 'custom',
                lang: '*',
                tokenizer: 'icu_tokenizer',
                filters: ["icu_folding", "icu_normalizer"],
                charFilters: ["html_strip"]
        )

        // en
        analysis.filters << new ESFilter(
                id: 'en_stop_filter',
                type:FILTER_TYPE.STOP,
                stopwords: ['_english_']
        )
        analysis.filters << new ESFilter(
                id: 'en_stem_filter',
                type:FILTER_TYPE.STEM,
                name: 'minimal_english'
        )
        analysis.index_analyzers << new ESAnalyzer(
                id: 'en_index_analyzer',
                lang: 'en',
                type: 'custom',
                tokenizer: 'icu_tokenizer',
                filters: ["icu_folding", "icu_normalizer", "en_stop_filter", "en_stem_filter", "nGram_filter"],
                charFilters: ["html_strip"]
        )
        analysis.search_analyzers << new ESAnalyzer(
                id: 'en_search_analyzer',
                lang: 'en',
                type: 'custom',
                tokenizer: 'icu_tokenizer',
                filters: ["icu_folding", "icu_normalizer", "en_stop_filter", "en_stem_filter"],
                charFilters: ["html_strip"]
        )

        // es
        analysis.filters << new ESFilter(
                id: 'es_stop_filter',
                type:FILTER_TYPE.STOP,
                stopwords: ['_spanish_']
        )
        analysis.filters << new ESFilter(
                id: 'es_stem_filter',
                type:FILTER_TYPE.STEM,
                name: 'light_spanish'
        )
        analysis.index_analyzers << new ESAnalyzer(
                id: 'es_index_analyzer',
                lang: 'es',
                type: 'custom',
                tokenizer: 'icu_tokenizer',
                filters: ["icu_folding", "icu_normalizer", "es_stop_filter", "es_stem_filter", "nGram_filter"],
                charFilters: ["html_strip"]
        )
        analysis.search_analyzers << new ESAnalyzer(
                id: 'es_search_analyzer',
                lang: 'es',
                type: 'custom',
                tokenizer: 'icu_tokenizer',
                filters: ["icu_folding", "icu_normalizer", "es_stop_filter", "es_stem_filter"],
                charFilters: ["html_strip"]
        )

        // fr
        analysis.filters << new ESFilter(
                id: 'fr_stop_filter',
                type:FILTER_TYPE.STOP,
                stopwords: ['_french_']
        )
        analysis.filters << new ESFilter(
                id: 'fr_stem_filter',
                type:FILTER_TYPE.STEM,
                name: 'minimal_french'
        )
        analysis.index_analyzers << new ESAnalyzer(
                id: 'fr_index_analyzer',
                lang: 'fr',
                type: 'custom',
                tokenizer: 'icu_tokenizer',
                filters: ["icu_folding", "icu_normalizer", "fr_stop_filter", "fr_stem_filter", "nGram_filter"],
                charFilters: ["html_strip"]
        )
        analysis.search_analyzers << new ESAnalyzer(
                id: 'fr_search_analyzer',
                lang: 'fr',
                type: 'custom',
                tokenizer: 'icu_tokenizer',
                filters: ["icu_folding", "icu_normalizer", "fr_stop_filter", "fr_stem_filter"],
                charFilters: ["html_strip"]
        )

        // ge
        analysis.filters << new ESFilter(
                id: 'de_stop_filter',
                type:FILTER_TYPE.STOP,
                stopwords: ['_german_']
        )
        analysis.filters << new ESFilter(
                id: 'de_stem_filter',
                type:FILTER_TYPE.STEM,
                name: 'minimal_german'
        )
        analysis.index_analyzers << new ESAnalyzer(
                id: 'de_index_analyzer',
                lang: 'de',
                type: 'custom',
                tokenizer: 'icu_tokenizer',
                filters: ["icu_folding", "icu_normalizer", "de_stop_filter", "de_stem_filter", "nGram_filter"],
                charFilters: ["html_strip"]
        )
        analysis.search_analyzers << new ESAnalyzer(
                id: 'de_search_analyzer',
                lang: 'de',
                type: 'custom',
                tokenizer: 'icu_tokenizer',
                filters: ["icu_folding", "icu_normalizer", "de_stop_filter", "de_stem_filter"],
                charFilters: ["html_strip"]
        )

        analysis
    }

    /**
     * This method performs a search on es
     * @param request - the request to perform on es
     * @param config - a configuration map for http client
     * @return search response
     */
    ESSearchResponse search(ESRequest request, Map config = [debug: true]) {
        def hits = []
        def aggregations = [:]
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
                        aggregations = data['aggregations']
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
        search(new ESRequest(request.properties))
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

class ESFilter{
    String id
    ESClient.FILTER_TYPE type
    String[] stopwords
    String name
    Map options
}

class ESAnalyzer{
    String id
    String lang
    String type
    String tokenizer
    String[] filters
    String[] charFilters
}

class ESTokenizer{
    String id
    String type
    String mode
}

class ESAnalysis{
    Collection<ESFilter> filters
    Collection<ESAnalyzer> index_analyzers
    Collection<ESAnalyzer> search_analyzers
    Collection<ESTokenizer> tokenizers
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
