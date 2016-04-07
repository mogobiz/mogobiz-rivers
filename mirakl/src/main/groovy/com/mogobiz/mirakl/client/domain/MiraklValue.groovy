package com.mogobiz.mirakl.client.domain

/**
 *
 * Created by smanciot on 08/04/16.
 */
class MiraklValue extends MiraklItem{

    MiraklValue root

    @Override
    StringBuffer append(StringBuffer buffer) {
        buffer.append(String.format("${root.id};${root.label};$id;$label;${action.toString().toLowerCase()}%n"))
    }

}
