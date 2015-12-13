/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.elasticsearch.rivers.mappings

import groovy.json.JsonSlurper

/**
 *
 */
final class ESMappings {

    private ESMappings(){}

    public static Map loadMappings(String name){
        final stream = ESMappings.class.getResourceAsStream("${name}.json")
        stream ? new JsonSlurper().parse(new InputStreamReader(stream)) as Map : null
    }
}
