package com.mogobiz.mirakl.client.domain

/**
 *
 * Created by smanciot on 03/04/16.
 */
class MiraklItems<T extends MiraklItem> {

    def List<T> items

    String header

    byte[] getBytes(String charset){
        def buffer = new StringBuffer(String.format("${header}%n"))
        items?.each {
            it.append(buffer)
        }
        buffer.toString().getBytes(charset)
    }
}
