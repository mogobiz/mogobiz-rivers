package com.mogobiz.rivers

import akka.stream.scaladsl._
import org.reactivestreams.Subscriber

import scala.util.Try


/**
 *
 * Created by smanciot on 14/08/14.
 */
package object cfp {

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

  def regrouped[T] = Flow[T].transform(() => new Fold[T, Seq[T]](Seq.empty, (out, in) => out :+ in)) //.scan[Seq[T]](Seq.empty)((out, in) => out :+ in)

  import akka.actor.ActorSystem
  import akka.event.Logging
  import akka.stream.stage._

  // Logging elements of a stream
  // mysource.transform(() => new LoggingStage(name))
  private[cfp] final class LoggingStage[T](private val name:String = "Logging")(implicit system:ActorSystem) extends PushStage[T, T] {
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

  private[cfp] final case class Fold[In, Out](zero: Out, f: (Out, In) => Out) extends PushPullStage[In, Out] {
    private var aggregator = zero

    override def onPush(elem: In, ctx: Context[Out]): Directive = {
      aggregator = f(aggregator, elem)
      ctx.pull()
    }

    override def onPull(ctx: Context[Out]): Directive =
      if (ctx.isFinishing) ctx.pushAndFinish(aggregator)
      else ctx.pull()

    override def onUpstreamFinish(ctx: Context[Out]): TerminationDirective = ctx.absorbTermination()
  }
}

package cfp {

trait CfpObject

trait CfpDetailsObject extends CfpObject{
  def url:String
}

case class CfpLinks(links: Seq[CfpLink]) extends CfpObject

case class CfpLink(href: String) extends CfpDetailsObject{
  override  val url = href
}

case class CfpConference(eventCode: String, label: String) extends CfpObject

case class CfpConferenceDetails(eventCode: String, label: String, speakers: Seq[CfpSpeakerDetails], schedules: Seq[CfpSchedule], avatars: Seq[CfpAvatar]) extends CfpObject{
  lazy val slots: Seq[CfpSlot] = schedules.flatMap((s) => s.slots.filter(_.talk match {
    case Some(_) => true
    case None => false
  }))
  def avatarFile(uuid: String) = avatars.find(_.uuid == uuid).map(_.file).getOrElse(None)
}

trait Speaker extends CfpObject{
  def uuid: String
  def avatarURL: String
}

case class CfpSpeaker(uuid:String,
                      firstName: String,
                      lastName: String,
                      avatarURL: String,
                      links: Seq[CfpLink]) extends CfpObject with Speaker{
  lazy val url = links.headOption.map(_.href).getOrElse(avatarURL)
}

case class CfpSpeakerDetails(
                              uuid: String,
                              firstName: String,
                              lastName: String,
                              avatarURL: String,
                              bioAsHtml:String,
                              company:String,
                              bio:String,
                              blog:String,
                              twitter:String,
                              lang:String) extends CfpObject with Speaker {
  lazy val name = s"$firstName $lastName"
}

case class CfpSchedule(slots: Seq[CfpSlot]) extends CfpObject {
  lazy val talks: Seq[CfpTalk] = slots.flatMap((s) => s.talk)
}

case class CfpSlot(slotId: String,
                   roomName: String,
                   roomCapacity: Long,
                   fromTime: String,
                   toTime: String,
                   fromTimeMillis: Long,
                   toTimeMillis: Long,
                   talk: Option[CfpTalk]) extends CfpObject

case class CfpTalk(
                    id: String,
                    title: String,
                    lang: String,
                    summary: String,
                    summaryAsHtml: String,
                    talkType: String,
                    track: String,
                    speakers: Seq[CfpTalkSpeaker]) extends CfpObject

case class CfpTalkSpeaker(link: CfpLink, name: String) extends CfpObject {
  lazy val uuid: Try[String] = Try{
    val href = link.href
    href.substring(href.lastIndexOf('/') + 1)
  }
}

case class CfpException(message:String) extends Exception(message)

case class CfpAvatar(uuid: String, file: Option[String])
}
