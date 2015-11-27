package com.mogobiz.elasticsearch.client

import groovy.json.JsonBuilder

import static scala.collection.JavaConversions.asScalaSet

/**
 *
 * Created by smanciot on 27/11/15.
 */
class EsAnalysisTest extends GroovyTestCase{

    void testApply(){
        final expected = """{
    "analysis": {
        "analyzer": {
            "default_index_analyzer": {
                "char_filter": [
                    "html_strip"
                ],
                "tokenizer": "icu_tokenizer",
                "type": "custom",
                "filter": [
                    "icu_folding",
                    "icu_normalizer",
                    "nGram_filter"
                ]
            },
            "default_search_analyzer": {
                "char_filter": [
                    "html_strip"
                ],
                "tokenizer": "icu_tokenizer",
                "type": "custom",
                "filter": [
                    "icu_folding",
                    "icu_normalizer"
                ]
            }
        },
        "tokenizer": {

        },
        "filter": {
            "nGram_filter": {
                "max_gram": 20,
                "min_gram": 2,
                "type": "nGram"
            }
        }
    }
}"""
        def languages = ["*"]
        def _analysis = EsAnalysis$.MODULE$.apply(asScalaSet(languages.toList().toSet()).toSet())
        JsonBuilder builder = new JsonBuilder()
        builder.call(_analysis.toJavaMap())
        assertEquals(expected, builder.toPrettyString())
    }
}
