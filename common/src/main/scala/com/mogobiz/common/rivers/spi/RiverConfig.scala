package com.mogobiz.common.rivers.spi

/**
  * Created by smanciot on 16/11/2017.
  */
import java.util

import com.mogobiz.common.client.ClientConfig

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

class RiverConfig{
  @BeanProperty var partial: Boolean = false
  @BeanProperty var idCompany: Long = -1L
  @BeanProperty var idCatalogs: util.List[Long] = List()
  @BeanProperty var debug: Boolean = false
  @BeanProperty var dry_run: Boolean = false
  @BeanProperty var languages: util.List[String] = List("fr", "en", "es", "de")
  @BeanProperty var defaultLang: String = "fr"
  @BeanProperty var countryCode: String = "FR"
  @BeanProperty var currencyCode: String = "EUR"
  @BeanProperty var clientConfig: ClientConfig = null
  @BeanProperty var countries: util.List[String] = List("DE","ES","FR","GB","US")
  @BeanProperty var idCategories: util.List[Long] = List()
  @BeanProperty var idProducts: util.List[Long] = List()
  @BeanProperty var bulkSize: Int = 100
}

