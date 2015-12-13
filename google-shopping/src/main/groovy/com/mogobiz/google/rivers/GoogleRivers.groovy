/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.google.rivers

import com.mogobiz.common.rivers.Rivers
import com.mogobiz.google.rivers.spi.GoogleRiver

/**
 */
class GoogleRivers extends Rivers<GoogleRiver>{

    static GoogleRivers instance

    private GoogleRivers(){super(GoogleRiver.class)}

    static GoogleRivers getInstance(){
        if(!instance){
            instance = new GoogleRivers()
        }
        instance
    }
}
