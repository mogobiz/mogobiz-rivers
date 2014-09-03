package com.mogobiz.common.rivers.spi

import com.mogobiz.common.client.ClientConfig

/**
 * Created by stephane.manciot@ebiznext.com on 14/05/2014.
 */
class RiverConfig {
    Long idCatalog
    boolean debug = true
    boolean dry_run = false
    List<String> languages = ['fr', 'en', 'es', 'de']
    String defaultLang = 'fr'
    String countryCode = 'FR'
    String currencyCode = 'EUR'
    ClientConfig clientConfig
}
