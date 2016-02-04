/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz.mirakl.rivers.spi

import com.mogobiz.common.rivers.spi.AbstractRiver
import com.mogobiz.mirakl.client.MiraklClient

/**
 *
 */
abstract class AbstractMiraklRiver<E> extends AbstractRiver<E, MiraklClient> implements MiraklRiver {
}
