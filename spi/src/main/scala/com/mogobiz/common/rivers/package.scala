/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.common

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl._
import akka.stream.stage.{Context, Directive, PushStage, TerminationDirective}
import org.reactivestreams.Subscriber

/**
 *
 */
package object rivers {

  def subscribe[T]: Source[T] => Seq[Subscriber[T]] => FlowGraph = source => subscribers => FlowGraph{implicit b =>
    import FlowGraphImplicits._
    if(subscribers.nonEmpty){
      if(subscribers.size > 1){
        val broadcast = Broadcast[T]
        source ~> broadcast
        for(subscriber <- subscribers){
          broadcast ~> SubscriberSink(subscriber)
        }
      }
      else{
        source ~> SubscriberSink(subscribers.head)
      }
    }
    else{
      source ~> Sink.ignore
    }
  }

  def flatten[T] = Flow[Seq[T]].map(_.toList).mapConcat(identity)

  // Logging elements of a stream
  // mysource.transform(() => new LoggingStage(name))
  private[rivers] final class LoggingStage[T](private val name:String = "Logging")(implicit system:ActorSystem) extends PushStage[T, T] {
    private val log = Logging(system, name)
    override def onPush(elem: T, ctx: Context[T]): Directive = {
      log.info(s"$name -> Element flowing through: {}", elem)
      ctx.push(elem)
    }
    override def onUpstreamFailure(cause: Throwable,
                                   ctx: Context[T]): TerminationDirective = {
      log.error(cause, s"$name -> Upstream failed.")
      super.onUpstreamFailure(cause, ctx)
    }
    override def onUpstreamFinish(ctx: Context[T]): TerminationDirective = {
      log.info(s"$name -> Upstream finished")
      super.onUpstreamFinish(ctx)
    }
  }

}
