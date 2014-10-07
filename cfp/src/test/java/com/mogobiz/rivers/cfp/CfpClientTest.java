package com.mogobiz.rivers.cfp;

import akka.japi.Procedure;
import static org.junit.Assert.*;

import akka.stream.javadsl.OnCompleteCallback;
import org.reactivestreams.api.Producer;
import akka.stream.javadsl.Flow;

import static scala.collection.JavaConversions.*;

import org.junit.Test;
import scala.collection.Seq;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by smanciot on 14/08/14.
 */
public class CfpClientTest {
    @Test
    public void testLoadAllConferences() throws InterruptedException {
        Producer<Seq<CfpConferenceDetails>> p = CfpClient.loadAllConferences("http://cfp.devoxx.fr");
        Flow.create(p).foreach(new Procedure<Seq<CfpConferenceDetails>>() {
            @Override
            public void apply(Seq<CfpConferenceDetails> conferences) throws Exception {
                assertEquals(1, conferences.size());
                for(CfpConferenceDetails param : asJavaCollection(conferences)){
                    assertEquals(3, param.schedules().size());
                    assertEquals(208, param.speakers().size());
                    for (Iterator<CfpSchedule> it = asJavaIterator(param.schedules().iterator()); it.hasNext(); ) {
                        CfpSchedule schedule = it.next();
                        List talks = new ArrayList<CfpTalk>();
                        for (CfpSlot slot : asJavaCollection(schedule.slots())) {
                            if(slot.talk().isDefined()){
                                CfpTalk talk = slot.talk().get();
                                talks.add(talk);
                                for (CfpTalkSpeaker speaker : asJavaCollection(talk.speakers())) {
                                    assertNotNull(speaker.uuid());
                                }
                            }
                        }
                        assertTrue(talks.size() > 0);
                    }
                }
            }
        }).onComplete(CfpClient.flowMaterializer(), new OnCompleteCallback(){
            public void onComplete(Throwable th){
                CfpClient.system().shutdown();
            }
        });
        Thread.sleep(20000);
    }
}
