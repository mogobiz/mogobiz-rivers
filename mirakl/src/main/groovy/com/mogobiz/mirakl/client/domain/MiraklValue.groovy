package com.mogobiz.mirakl.client.domain

/**
 *
 * Created by smanciot on 08/04/16.
 */
class MiraklValue extends MiraklItem{

    MiraklValue root

    @Override
    StringBuffer append(StringBuffer buffer, String separator = ";") {
        buffer.append(String.format("${root.id}$separator${root.label}$separator$id$separator$label$separator${action.toString().toLowerCase()}%n"))
    }

}
