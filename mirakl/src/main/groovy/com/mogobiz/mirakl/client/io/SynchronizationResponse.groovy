/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz.mirakl.client.io

/**
 *
 */
interface SynchronizationResponse {

    Long getSynchroId()

    List<String> getIds()
}
