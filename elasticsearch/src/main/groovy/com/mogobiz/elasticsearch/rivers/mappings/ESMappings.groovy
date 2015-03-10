package com.mogobiz.elasticsearch.rivers.mappings

import groovy.json.JsonSlurper

/**
 *
 * Created by smanciot on 10/03/15.
 */
final class ESMappings {

    private ESMappings(){}

    public static Map loadMappings(String name){
        new JsonSlurper().parse(new InputStreamReader(ESMappings.class.getResourceAsStream("${name}.json")))
    }
}
