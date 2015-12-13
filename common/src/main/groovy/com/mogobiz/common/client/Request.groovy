/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.common.client

/**
 */
class Request {
    ClientConfig clientConfig
    String type
    Map query = [:]
    List<String> included
    List<String> excluded
}
