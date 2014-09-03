package com.mogobiz.rivers.cfp

import akka.stream.{MaterializerSettings, FlowMaterializer}
import akka.stream.scaladsl.Flow
import org.reactivestreams.api.Producer

import scala.concurrent.Future
import scala.concurrent._
import scala.util.{Failure, Success}

/**
 *
 * Created by smanciot on 14/08/14.
 */
object CfpClient extends BootedCfpSystem {

  import spray.http._
  import CfpJsonProtocol._
  import spray.client.pipelining._

  def pipeline[T](implicit m:Manifest[T], ec:ExecutionContext): HttpRequest => Future[T] = (
    addHeader("Accept", "application/json")
      ~> sendReceive
      ~> unmarshal[T]
    )

  def getObjects[T](url:String)(implicit m:Manifest[T], ec:ExecutionContext):Future[T] = pipeline[T](m, ec)(Get(url))

  def getObjectsFn[T](implicit m: Manifest[T], ec:ExecutionContext): String => Future[T] = getObjects[T]

  def getFutureDetails[T, B](url:String)(implicit m1: Manifest[T], m2: Manifest[B], ev:B => Seq[CfpDetailsObject], ec:ExecutionContext):Future[Seq[T]] = {
    val p = Promise[Seq[T]]()

    getObjectsFn(m2, ec).andThen((g:Future[B]) => g onComplete {
      case Success(b) =>
        Future.sequence(ev(b).map((link) => getObjects(link.url)(m1, ec))) onComplete{
          case Success(s) =>
            p.trySuccess(s)
          case Failure(f) =>
            p.failure(f)
        }
      case Failure(f) =>
        p.failure(f)
    })(url)

    p.future
  }

  val flowMaterializer: FlowMaterializer = FlowMaterializer(MaterializerSettings())

  def loadAllConferences(url:String):Producer[Seq[CfpConferenceDetails]] = {

    implicit val ec: ExecutionContext = system.dispatcher

    implicit def links2Url(links:CfpLinks) : Seq[String] = links.links.map(_.href)

    Flow(getObjects[CfpLinks](s"$url/api/conferences")).mapFuture(urls => Future.sequence(for(url <- urls) yield loadConference(url))).toProducer(flowMaterializer)

  }

  def loadConference(url:String)(implicit ec:ExecutionContext):Future[CfpConferenceDetails] = {
    val p = Promise[CfpConferenceDetails]()

    for(schedules <- loadSchedules(s"$url/schedules");
        speakers <- loadSpeakers(s"$url/speakers");
        s <- getObjects[CfpConference](url)
    ) {
      p.trySuccess(CfpConferenceDetails(s.eventCode, s.label, speakers, schedules))
    }

    p.future
  }

  def loadSpeakers(url:String)(implicit ec:ExecutionContext):Future[Seq[CfpSpeakerDetails]] = {

    implicit val uuid2Pipeline : Seq[CfpSpeaker] => Seq[CfpDetailsObject] = cfpUuids => cfpUuids.map((u) => CfpLink(s"$url/${u.uuid}"))

    getFutureDetails[CfpSpeakerDetails, Seq[CfpSpeaker]](url)

  }

  def loadSchedules(url:String)(implicit ec:ExecutionContext):Future[Seq[CfpSchedule]] = {

    implicit val links2Pipeline : CfpLinks => Seq[CfpDetailsObject] = cfpLinks => cfpLinks.links

    getFutureDetails[CfpSchedule, CfpLinks](url)

  }

}
