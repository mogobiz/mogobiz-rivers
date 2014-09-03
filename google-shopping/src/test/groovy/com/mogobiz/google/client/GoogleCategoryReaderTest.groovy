package com.mogobiz.google.client

import akka.actor.ActorSystem
import akka.dispatch.Futures
import akka.dispatch.Mapper
import rx.util.functions.Action0
import rx.util.functions.Action1
import rx.util.functions.Func1
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import java.util.concurrent.Callable

import static java.util.concurrent.TimeUnit.SECONDS

/**
 * Created by smanciot on 04/06/14.
 */
class GoogleCategoryReaderTest extends GroovyTestCase{

    private static final ActorSystem GOOGLE_SYSTEM = ActorSystem.create("GOOGLE")

    void testParseCategories(){

        def categories = GoogleCategoryReader.parseCategories()

        List<Future<Integer>> futures = new ArrayList<Future<Integer>>()

        long expected = 0
        def onNext = {expected++; futures << it} as Action1<Integer>
        def onError = {Throwable th -> expected++;log.info(th.message)} as Action1<Throwable>
        def onCompleted = {} as Action0

        ExecutionContext ec = GOOGLE_SYSTEM.dispatcher()

        categories.flatMap({GoogleCategoryItem item ->
            rx.Observable.from(Futures.future(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return item?.tokens?.size() > 0 ? 1 : 0
                }
            }, ec))
        } as Func1).subscribe(onNext, onError, onCompleted)

        // compose a sequence of the futures
        Future<Iterable<Integer>> futuresSequence = Futures.sequence(futures, ec)

        Future<Long> futureResult = futuresSequence.map(new Mapper<Iterable<Integer>, Long>(){
            @Override
            public Long apply(Iterable<Integer> results) {
                Long total = 0
                results.each {
                    total += it
                }
                return total
            }
        }, ec)

        Long total = Await.result(futureResult, Duration.create(30, SECONDS))

        log.info('recorded ' + total + '/' + expected + ' lines')

        assertEquals(expected, total)
    }
}
