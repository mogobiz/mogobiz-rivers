package com.mogobiz.google.rivers

import com.mogobiz.common.rivers.Rivers
import com.mogobiz.google.rivers.spi.GoogleRiver

/**
 * Created by smanciot on 16/05/2014.
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
