/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz.mirakl.client.io;

import java.util.List;

/**
 *
 */
public interface ImportResponse {

    public Long getImportId();

    public List<String> getIds();
}
