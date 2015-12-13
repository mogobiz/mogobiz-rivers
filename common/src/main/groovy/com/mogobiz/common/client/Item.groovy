/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.common.client

/**
 */
class Item {
    String id
    Item parent
    List<Item> children = []
    String type
    Map<String, Object> map
}
