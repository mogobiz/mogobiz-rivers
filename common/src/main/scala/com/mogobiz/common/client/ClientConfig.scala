/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.common.client

import java.util
import java.util.Date

import scala.collection.JavaConversions._
import scala.beans.BeanProperty

/**
 *
 */
class ClientConfig{
  /**
    * id store
    */
  @BeanProperty var store: String = null
  @BeanProperty var debug: Boolean = true
  /**
    * merchant id - google shopping
    */
  @BeanProperty var merchant_id: String = null
  /**
    * merchant url - google shopping
    */
  @BeanProperty var merchant_url: String = null
  @BeanProperty var credentials: Credentials = null
  @BeanProperty var config: util.Map[String, AnyRef] = Map[String, AnyRef]()
  @BeanProperty var url: String = null
}

class Credentials{
  /**
   * front key - mirakl
   */
  @BeanProperty var frontKey: String = null
  /**
   * api key - mirakl
   */
  @BeanProperty var apiKey: String = null
  @BeanProperty var client_id: String = null
  @BeanProperty var client_secret: String = null
  @BeanProperty var client_token: String = null
  @BeanProperty var expiration: util.Date = null
  def refreshToken(): Boolean = {
    Option(client_token).isEmpty || new Date().after(expiration)
  }
}
