package com.mogobiz.common.client

/**
 * Created by smanciot on 14/05/2014.
 */
class Item {
    String id
    Item parent
    List<Item> children = []
    String type
    Map<String, Object> map
}
