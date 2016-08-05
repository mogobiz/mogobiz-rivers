/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.common.rivers

import java.util

import akka.dispatch.Futures
import com.mogobiz.common.rivers.spi.{GenericRiver, RiverConfig}
import org.reactivestreams.Publisher
import rx.Observable
import rx.functions.{Action1, Func0}
import rx.internal.reactivestreams.PublisherAdapter

import scala.concurrent.{Future, ExecutionContext}

import scala.collection.JavaConversions._

/**
 *
 */
trait GenericRivers[In, Out] extends BootedRiversSystem{

  def loadRivers: util.List[GenericRiver[In, Out]]

  def dispatcher: ExecutionContext = system.dispatcher

  def publisher(config: RiverConfig): Publisher[In] = {
    new PublisherAdapter[In](
      Observable.defer(
        new Func0[Observable[In]]() {
          override def call: Observable[In] =
            Observable.merge(loadRivers.map(_.exportCatalogItemsAsRiverItems(config)))
        }
      )
    )
  }

  import java.util

  def bulk(config: RiverConfig, items: util.List[In], ec: ExecutionContext): Future[Out]

  final def collect(futures: util.List[Future[Out]]): Future[util.Iterator[Out]] = {
    Futures.sequence(futures.toList, dispatcher).map(_.iterator())(dispatcher)
  }

  def export(config: RiverConfig) = {
    var futures: List[Future[Out]] = List.empty
    Observable.merge(loadRivers.map(_.exportCatalogItems(config, dispatcher))).subscribe(new Action1[Future[Out]]() {
      override def call(future: Future[Out]): Unit = futures ++= List(future)
    }, new Action1[Throwable]() {
      override def call(t: Throwable): Unit = t.printStackTrace(System.err)
    })
    collect(futures)
  }
}

abstract class AbstractGenericRivers[In, Out] extends GenericRivers[In, Out]
