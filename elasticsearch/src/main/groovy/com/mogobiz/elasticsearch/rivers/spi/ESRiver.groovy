/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.elasticsearch.rivers.spi

import com.mogobiz.common.rivers.spi.River
import com.mogobiz.elasticsearch.client.ESMapping

/**
 *
 */
public interface ESRiver extends River{

    ESMapping defineESMapping()

}
