package com.mogobiz.rivers

import scala.util.Try


/**
 *
 * Created by smanciot on 14/08/14.
 */
package object cfp {

  type CfpConsumer = CfpConference => Unit

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

case class CfpConferenceDetails(eventCode: String, label: String, speakers: Seq[CfpSpeakerDetails], schedules: Seq[CfpSchedule]) extends CfpObject{
  lazy val slots: Seq[CfpSlot] = schedules.flatMap((s) => s.slots.filter(_.talk match {
    case Some(_) => true
    case None => false
  }))
}

case class CfpSpeaker(uuid:String,
                      firstName: String,
                      lastName: String,
                      avatarURL: String) extends CfpObject

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
                              lang:String) extends CfpObject {
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

case class CfpAvatar(id: String, url: String)
}
