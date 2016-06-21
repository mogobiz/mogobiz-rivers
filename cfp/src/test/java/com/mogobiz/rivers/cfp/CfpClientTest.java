package com.mogobiz.rivers.cfp;

import static org.junit.Assert.*;

import akka.stream.javadsl.*;
import akka.stream.javadsl.japi.Function;
import akka.stream.javadsl.japi.Procedure;

import static scala.collection.JavaConversions.*;

import org.junit.Test;
import rx.Subscriber;
import rx.internal.reactivestreams.SubscriberAdapter;
import scala.collection.Seq;
import scala.runtime.BoxedUnit;

import java.util.*;

/**
 *
 */
public class CfpClientTest {
    @Test
    public void testLoadAllConferences() throws InterruptedException {
        Source<Integer> source = Source.adapt(CfpClient.loadAllConferences("http://cfp.devoxx.fr")).map(new Function<CfpConferenceDetails, Integer>() {
            public Integer apply(CfpConferenceDetails conference) throws Exception {
                final Seq<CfpSchedule> schedules = conference.schedules();
                assertEquals(5, schedules.size());
                final Seq<CfpSpeakerDetails> speakers = conference.speakers();
                assertEquals(182, speakers.size());
                for (Iterator<CfpSchedule> it = asJavaIterator(schedules.iterator()); it.hasNext(); ) {
                    CfpSchedule schedule = it.next();
                    final Seq<CfpSlot> slots = schedule.slots();
                    if (slots.size() > 0) {
                        List<CfpTalk> talks = new ArrayList<CfpTalk>();
                        boolean found = false;
                        for (CfpSlot slot : asJavaCollection(slots)) {
                            if (slot.talk().isDefined()) {
                                found = true;
                                CfpTalk talk = slot.talk().get();
                                talks.add(talk);
                                for (CfpTalkSpeaker speaker : asJavaCollection(talk.speakers())) {
                                    assertNotNull(speaker.uuid());
                                }
                            }
                        }
                        if (found) assertTrue(talks.size() > 0);
                    }
                }
                return schedules.size();
            }
        });
        Sink<Integer> sink = Sink.onComplete(new Procedure<BoxedUnit>(){

            @Override
            public void apply(BoxedUnit param) throws Exception {
                System.out.println("OK");
            }
        });
        source.runWith(sink, CfpClient.flowMaterializer());
        Thread.sleep(15000);
    }

    @Test
    public void testLoadAllConferencesWithSubscriber() throws InterruptedException {
        Subscriber<CfpConferenceDetails> subscriber = new Subscriber<CfpConferenceDetails>() {
            @Override
            public void onCompleted() {
                System.out.println("OK");
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println(throwable.getMessage());
            }

            @Override
            public void onNext(CfpConferenceDetails conference) {
                final Seq<CfpSpeakerDetails> speakers = conference.speakers();
                assertEquals(184, speakers.size());

                final Seq<CfpAvatar> avatars = conference.avatars();
                assertEquals(184, avatars.size());
//                for (Iterator<CfpAvatar> it = asJavaIterator(avatars.iterator()); it.hasNext(); ) {
//                    CfpAvatar avatar = it.next();
//                    System.out.println("avatar " + avatar.uuid() + " -> " + avatar.file().get());
//                }

                final Seq<CfpSchedule> schedules = conference.schedules();
                assertEquals(5, schedules.size());
                for (Iterator<CfpSchedule> it = asJavaIterator(schedules.iterator()); it.hasNext(); ) {
                    CfpSchedule schedule = it.next();
                    final Seq<CfpSlot> slots = schedule.slots();
                    if (slots.size() > 0) {
                        List<CfpTalk> talks = new ArrayList<CfpTalk>();
                        boolean found = false;
                        for (CfpSlot slot : asJavaCollection(slots)) {
                            if (slot.talk().isDefined()) {
                                found = true;
                                CfpTalk talk = slot.talk().get();
                                talks.add(talk);
                                for (CfpTalkSpeaker speaker : asJavaCollection(talk.speakers())) {
                                    assertNotNull(speaker.uuid());
                                }
                            }
                        }
                        if (found) assertTrue(talks.size() > 0);
                    }
                }
            }
        };
        CfpClient.loadAllConferences("http://cfp.devoxx.be", new SubscriberAdapter<CfpConferenceDetails>(subscriber));
        Thread.sleep(15000);
    }
}
