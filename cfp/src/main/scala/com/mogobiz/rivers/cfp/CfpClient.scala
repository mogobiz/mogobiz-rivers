package com.mogobiz.rivers.cfp

import java.io.{FileOutputStream, File}

import akka.stream.{ActorFlowMaterializer, FlowMaterializer}
import akka.stream.scaladsl._
import org.reactivestreams.Subscriber

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

  implicit val flowMaterializer: FlowMaterializer = ActorFlowMaterializer()

  implicit val ec: ExecutionContext = system.dispatcher

  def subscribe[T]: Source[T] => Seq[Subscriber[T]] => Unit = source => subscribers => FlowGraph{implicit b =>
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
  }.run()

  def loadAllConferences(url:String, subscriber: Subscriber[CfpConferenceDetails]): Unit = {
    subscribe(loadAllConferences(url))(Seq(subscriber))
  }

  def loadAllConferences(url:String): Source[CfpConferenceDetails] = {
    Source(getObjects[CfpLinks](s"$url/api/conferences"))
      .map[List[String]](_.links.map(_.href).toList)
      .mapConcat[String](identity)
      .mapAsyncUnordered(loadConference)
  }

  def downloadAvatars(avatars: Seq[CfpAvatar], destination: File, subscriber: Subscriber[String]): Unit = {
    subscribe(downloadAvatars(avatars, destination))(Seq(subscriber))
  }

  def downloadAvatars(avatars: Seq[CfpAvatar], destination: File): Source[String] = {
    destination.mkdirs()
    if(destination.exists() && destination.isDirectory && destination.canWrite){
      Source(){implicit b =>
        import FlowGraphImplicits._
        import dispatch._
        val source: Source[CfpAvatar] = Source(avatars.toList).transform(() => new LoggingStage[CfpAvatar]())
        val balance = Balance[CfpAvatar]
        val merge = Merge[String]
        val read: Flow[CfpAvatar, (String, Option[Array[Byte]])] = Flow[CfpAvatar].map{ u =>
          val req = url(u.url)
          val res = Http(req > as.Bytes).option
          (u.id, res())
        }
        val write: Flow[(String, Option[Array[Byte]]), String] = Flow[(String, Option[Array[Byte]])].map{u =>
          val uuid = u._1
          u._2.map {bytes =>
            val file = new File(destination, uuid)
            val fos = new FileOutputStream(file)
            fos.write(bytes)
            file.getAbsolutePath
          }.getOrElse(s"*** avatar could not be downloaded for $uuid")
        }
        val sink = UndefinedSink[String]
        source ~> balance
        1 to 10 foreach {_ =>
          balance ~> read ~> write ~> merge
        }
        merge ~> sink
        sink
      }
    }
    else{
      Source.empty()
    }
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

  import akka.actor.ActorSystem
  import akka.event.Logging
  import akka.stream.stage._

  // Logging elements of a stream
  // mysource.transform(() => new LoggingStage(name))
  class LoggingStage[T](private val name:String = "Logging")(implicit system:ActorSystem) extends PushStage[T, T] {
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
