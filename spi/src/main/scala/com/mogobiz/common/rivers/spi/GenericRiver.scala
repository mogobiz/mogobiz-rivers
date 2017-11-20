/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.common.rivers.spi

import java.util
import java.text.SimpleDateFormat

import com.mogobiz.common.client._
import org.reactivestreams.Publisher
import rx.Observable
import rx.functions.Func1
import rx.internal.reactivestreams.PublisherAdapter

import scala.concurrent.{Future, ExecutionContext}

trait Transformation[A <: AnyRef, B <: AnyRef] {
  def asRiverItem(e: A, config: RiverConfig): B
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

  @deprecated
  def exportCatalogItems(config: RiverConfig, ec: ExecutionContext): Observable[Future[Out]] = {
    exportCatalogItemsAsRiverItems(config).buffer(config.bulkSize).flatMap(new Func1[util.List[In], Observable[Future[Out]]](){
      override def call(items: util.List[In]): Observable[Future[Out]] = Observable.just(bulk(config, items, ec))
    })
  }

  def retrieveCatalogItems(config: RiverConfig): Observable[AnyRef]

  def bulk(config: RiverConfig, items: util.List[In], ec: ExecutionContext): Future[Out]

  def publisher(config: RiverConfig): Publisher[In] = {
    new PublisherAdapter[In](exportCatalogItemsAsRiverItems(config))
  }

  def getType: String
}

abstract class AbstractGenericRiver[In, Out] extends GenericRiver[In, Out]

trait River extends GenericRiver[BulkItem, BulkResponse]

trait RiverItem {
  def getKey: String
  def asBulkItem(config: RiverConfig): BulkItem
}

abstract class AbstractRiver[T <: AnyRef] extends AbstractGenericRiver[BulkItem, BulkResponse] with River{

  override def asRiverItem(e: AnyRef, config: RiverConfig): BulkItem = {
    val `type` = getType
    val uuid = getUuid(e.asInstanceOf[T])
    new RiverItem {
      override def asBulkItem(config: RiverConfig): BulkItem = {
        val item = asItem(e.asInstanceOf[T], config)
        val id = item.getId
        val map = item.getMap
        map.put("imported", formatToIso8601(new util.Date()))
        val bulkItem = new BulkItem
        bulkItem.setAction(BulkAction.UPDATE)
        bulkItem.setId(id)
        bulkItem.setMap(map)
        bulkItem.setParent(item.getParent)
        bulkItem.setType(`type`)
        bulkItem
      }

      override def getKey: String = s"${`type`}::$uuid"
    }.asBulkItem(config)
  }

  def asItem(t: T, config: RiverConfig): Item

  def getUuid(t: T): String = t.toString

  def formatToIso8601(d: util.Date) = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss\'Z\'").format(d)
}