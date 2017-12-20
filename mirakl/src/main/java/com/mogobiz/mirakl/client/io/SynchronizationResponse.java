/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz.mirakl.client.io;

import java.util.List;

/**
 *
 */
public interface SynchronizationResponse {

    public Long getSynchroId();

    public List<String> getIds();
}
