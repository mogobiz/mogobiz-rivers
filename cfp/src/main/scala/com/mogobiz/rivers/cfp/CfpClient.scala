/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.rivers.cfp

import java.io.{FileOutputStream, File}

import akka.stream.{ActorFlowMaterializer, FlowMaterializer}
import akka.stream.scaladsl._
import com.mogobiz.tools.MimeTypeTools
import org.reactivestreams.Subscriber

import scala.concurrent.Future
import scala.concurrent._
import scala.util._

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

  implicit val flowMaterializer: FlowMaterializer = ActorFlowMaterializer()

  implicit val ec: ExecutionContext = system.dispatcher

  def loadAllConferences(url:String, subscriber: Subscriber[CfpConferenceDetails]): Unit = {
    subscribe(loadAllConferences(url))(Seq(subscriber)).run()
  }

  def loadAllConferences(url:String): Source[CfpConferenceDetails] = {
    Source(){implicit b =>
      import FlowGraphImplicits._

      val source = Source(getObjects[CfpLinks](s"$url/api/conferences"))
        .map[List[String]](_.links.map(_.href).toList)
        .mapConcat[String](identity)
      val broadcast = Broadcast[String]
//      val schedules = Flow[String].mapAsync[Seq[CfpSchedule]](u => loadSchedules(s"$u/schedules"))
//      val speakers = Flow[String].mapAsync[Seq[CfpSpeakerDetails]](u => loadSpeakers(s"$u/speakers"))
      val zip1 = Zip[Seq[CfpSchedule], (Seq[CfpSpeakerDetails], Seq[CfpAvatar])]
      val broadcast2 = Broadcast[Seq[CfpSpeakerDetails]]
      val zip2 = Zip[Seq[CfpSpeakerDetails], Seq[CfpAvatar]]
      val zip3 = ZipWith[
        (Seq[CfpSchedule], (Seq[CfpSpeakerDetails], Seq[CfpAvatar])),
        CfpConference,
        CfpConferenceDetails]((t, s) => CfpConferenceDetails(s.eventCode, s.label, t._2._1, t._1, t._2._2))

      val conference = Flow[String].mapAsync(getObjects[CfpConference])

      val undefinedSink = UndefinedSink[CfpConferenceDetails]

      source ~> broadcast ~> schedules ~> zip1.left
                broadcast ~> speakers  ~> broadcast2 ~> zip2.left
                                          broadcast2 ~> downloadAvatars(None) ~> zip2.right
                broadcast ~> conference ~> zip3.right

      zip2.out ~> zip1.right
      zip1.out ~> zip3.left
      zip3.out ~> undefinedSink

      undefinedSink
    }
  }

  def downloadAvatars(speakers: Seq[Speaker], destination: Option[File], subscriber: Subscriber[Seq[CfpAvatar]]): Unit = {
    subscribe(Source.single(speakers).via(downloadAvatars(destination)))(Seq(subscriber)).run()
  }

  def downloadAvatars(destination: Option[File]) = Flow(){implicit b =>
    import FlowGraphImplicits._
    import dispatch._
    val dir = destination.getOrElse(new File(s"${System.getProperty("java.io.tmpdir")}/cfp/${System.currentTimeMillis()}"))
    dir.mkdirs()
    val undefinedSource = UndefinedSource[Seq[Speaker]]
    val balance = Balance[Speaker]
    val merge = Merge[CfpAvatar]
    val read: Flow[Speaker, (String, Option[Array[Byte]])] = Flow[Speaker].map{ u =>
      val req = url(u.avatarURL)
      val res = Try(Http(req > as.Bytes).option)
      (u.uuid, res.map(r => r()).getOrElse(None))
    }
    val write: Flow[(String, Option[Array[Byte]]), CfpAvatar] = Flow[(String, Option[Array[Byte]])].map{u =>
      val uuid = u._1
      u._2.map {bytes =>
        val file = new File(dir, uuid)
        val fos = new FileOutputStream(file)
        fos.write(bytes)

        val from = file.getAbsolutePath
        val format = MimeTypeTools.toFormat(file)
        if(format != null){
          val to = from + "." + format
          import java.nio.file.StandardCopyOption.REPLACE_EXISTING
          import java.nio.file.Files._
          import java.nio.file.Paths.get
          move(get(from), get(to), REPLACE_EXISTING)
          CfpAvatar(uuid, Some(to))
        }
        else{
          CfpAvatar(uuid, Some(from))
        }
      }.getOrElse(CfpAvatar(uuid, None))
    }.transform(() => new LoggingStage[CfpAvatar]("avatar"))
    val undefinedSink = UndefinedSink[Seq[CfpAvatar]]
    if(dir.exists() && dir.isDirectory && dir.canWrite) {
      undefinedSource ~> flatten[Speaker] ~> balance
      1 to 10 foreach { _ =>
        balance ~> read ~> write ~> merge
      }
      merge ~> regrouped[CfpAvatar] ~> undefinedSink
    }
    (undefinedSource, undefinedSink)
  }

  @deprecated
  def loadSpeakers(url:String)(implicit ec:ExecutionContext):Future[Seq[CfpSpeakerDetails]] = {

    implicit val uuid2Pipeline : Seq[CfpSpeaker] => Seq[CfpDetailsObject] = cfpUuids => cfpUuids.map((u) => CfpLink(s"$url/${u.uuid}"))

    getFutureDetails[CfpSpeakerDetails, Seq[CfpSpeaker]](url)

  }

  @deprecated
  def loadSchedules(url:String)(implicit ec:ExecutionContext):Future[Seq[CfpSchedule]] = {

    implicit val links2Pipeline : CfpLinks => Seq[CfpDetailsObject] = cfpLinks => cfpLinks.links

    getFutureDetails[CfpSchedule, CfpLinks](url)

  }

  val speakers = Flow(){implicit b =>
    import FlowGraphImplicits._

    val undefinedSource = UndefinedSource[String]

    val loadUuids = Flow[String].transform(() => new LoggingStage[String]("speakers")).mapAsync{u => getObjects[Seq[CfpSpeaker]](s"$u/speakers")}
    val balance = Balance[CfpSpeaker]
    val loadSpeaker = Flow[CfpSpeaker].transform(() => new LoggingStage[CfpSpeaker]("speaker")).mapAsync[CfpSpeakerDetails]{link => getObjects[CfpSpeakerDetails](link.url)}
    val merge = Merge[CfpSpeakerDetails]

    val undefinedSink = UndefinedSink[Seq[CfpSpeakerDetails]]

    undefinedSource ~> loadUuids ~> flatten[CfpSpeaker] ~> balance

    1 to 5 foreach {_ =>
      balance ~> loadSpeaker ~> merge
    }

    merge ~> regrouped[CfpSpeakerDetails] ~> undefinedSink

    (undefinedSource, undefinedSink)
  }

  val schedules = Flow(){implicit b =>
    import FlowGraphImplicits._

    val undefinedSource = UndefinedSource[String]

    val loadLinks = Flow[String].mapAsync{u => getObjects[CfpLinks](s"$u/schedules")}.map(_.links)
    val loadSchedule = Flow[CfpLink].transform(() => new LoggingStage[CfpLink]("schedule")).mapAsync[CfpSchedule]{link => getObjects[CfpSchedule](link.url)}
    val balance = Balance[CfpLink]
    val merge = Merge[CfpSchedule]

    val undefinedSink = UndefinedSink[Seq[CfpSchedule]]

    undefinedSource ~> loadLinks ~> flatten[CfpLink] ~> balance

    1 to 5 foreach {_ =>
      balance ~> loadSchedule ~> merge
    }

    merge ~> regrouped[CfpSchedule] ~> undefinedSink

    (undefinedSource, undefinedSink)
  }

}
