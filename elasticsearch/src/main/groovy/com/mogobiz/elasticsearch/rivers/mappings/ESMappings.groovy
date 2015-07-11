package com.mogobiz.elasticsearch.rivers.mappings

import groovy.json.JsonSlurper

/**
 *
 * Created by smanciot on 10/03/15.
 */
final class ESMappings {

    private ESMappings(){}

    public static Map loadMappings(String name){
        final stream = ESMappings.class.getResourceAsStream("${name}.json")
        stream ? new JsonSlurper().parse(new InputStreamReader(stream)) as Map : null
    }
}
