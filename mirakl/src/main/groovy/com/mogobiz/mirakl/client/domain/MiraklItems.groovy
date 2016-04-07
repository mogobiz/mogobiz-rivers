package com.mogobiz.mirakl.client.domain

import groovy.util.logging.Slf4j

/**
 *
 * Created by smanciot on 03/04/16.
 */
@Slf4j
class MiraklItems<T extends MiraklItem> {

    def List<T> items

    String header

    byte[] getBytes(String charset){
        def buffer = new StringBuffer(String.format("${header}%n"))
        items?.each {
            it.append(buffer)
        }
        def str = buffer.toString()
        log.info(str)
        str.getBytes(charset)
    }
}
