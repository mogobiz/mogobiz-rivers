/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.common.rivers.spi

import com.mogobiz.common.client.ClientConfig
import org.reactivestreams.Publisher
import rx.Observable
import rx.functions.Func1
import rx.internal.reactivestreams.PublisherAdapter

import scala.beans.BeanProperty
import scala.concurrent.{Future, ExecutionContext}

trait Transformation[E, In] {
  def asRiverItem(e:E, config: RiverConfig): In
}

/**
  *
  */
trait GenericRiver[In, Out] extends Transformation[AnyRef, In]{
  def exportCatalogItemsAsRiverItems(config: RiverConfig): Observable[In] = {
    retrieveCatalogItems(config).flatMap(new Func1[AnyRef, Observable[In]](){
      override def call(e: AnyRef): Observable[In] = Observable.just(asRiverItem(e, config))
    })
  }

  import java.util

  @deprecated
  def exportCatalogItems(config: RiverConfig, ec: ExecutionContext, count:Int = 100): Observable[Future[Out]] = {
    exportCatalogItemsAsRiverItems(config).buffer(count).flatMap(new Func1[util.List[In], Observable[Future[Out]]](){
      override def call(items: util.List[In]): Observable[Future[Out]] = Observable.just(bulk(config, items, ec))
    })
  }

  def retrieveCatalogItems(config: RiverConfig): Observable[AnyRef]

  import java.util

  def bulk(config: RiverConfig, items: util.List[In], ec: ExecutionContext): Future[Out]

  def publisher(config: RiverConfig): Publisher[In] = {
    new PublisherAdapter[In](exportCatalogItemsAsRiverItems(config))
  }

  def getType: String
}

abstract class AbstractGenericRiver[In, Out] extends GenericRiver[In, Out]

import java.util
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
}
