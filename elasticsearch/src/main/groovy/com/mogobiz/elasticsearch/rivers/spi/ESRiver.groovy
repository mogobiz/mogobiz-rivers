/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.elasticsearch.rivers.spi

import com.mogobiz.common.rivers.spi.River
import com.mogobiz.elasticsearch.client.ESMapping

/**
 *
 * Created by stephane.manciot@ebiznext.com on 16/02/2014.
 */
public interface ESRiver extends River{

    ESMapping defineESMapping()

}
