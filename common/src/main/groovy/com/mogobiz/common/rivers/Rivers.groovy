package com.mogobiz.common.rivers

import akka.actor.ActorSystem
import akka.dispatch.Futures
import akka.dispatch.Mapper
import com.mogobiz.common.rivers.spi.River
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.common.rivers.spi.RiverItem
import org.reactivestreams.Publisher
import rx.Observable
import rx.functions.Action1
import rx.functions.Func0
import rx.functions.Func1
import rx.internal.reactivestreams.ObservableToPublisherAdapter
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 * Created by smanciot on 16/05/2014.
 */
abstract class Rivers<T extends River> {

    static final ActorSystem RIVERS = ActorSystem.create("RIVERS")

    /**
     * rivers
     */
    private final Map<String, T> rivers = new HashMap<String, T>()

    /**
     * service loader
     */
    private final ServiceLoader<T> riverLoader

    /**
     *
     */
    protected Rivers(Class<T> riverType){
        this.riverLoader = ServiceLoader.load(riverType)
        this.loadRivers()
    }

    static ExecutionContext dispatcher(){
        RIVERS.dispatcher()
    }

    /**
     * Discover and register the available rivers
     */
    public List<T> loadRivers(){
        rivers.clear()
        riverLoader.reload()
        for(T river : riverLoader.iterator()){
            rivers.put(river.type, river)
        }
        rivers.values() as List<T>
    }


    T loadRiver(String type){
        return rivers.containsKey(type) ? rivers.get(type) : null
    }

    Future<Collection<BulkResponse>> export(RiverConfig config, ExecutionContext ec = dispatcher()){
        Collection<Observable<Future<BulkResponse>>> iterable = []

        rivers.values().each {river ->
            iterable << river.exportCatalogItems(config, ec, 100)
        }

        Collection<Future<BulkResponse>> collection = []

        Observable.merge(iterable).subscribe(
                {Future<BulkResponse> response ->
                    collection << response
                } as Action1<Future<BulkResponse>>,
                {
                    th -> th.printStackTrace(System.err)
                } as Action1<Throwable>
        )

        collect(collection, ec)

    }


    static Future<Collection<BulkResponse>> collect(Collection<Future<BulkResponse>> futures, ExecutionContext ec){
        // compose a sequence of the futures
        Future<Iterable<BulkResponse>> futuresSequence = Futures.sequence(futures, ec)

        Future<Collection<BulkResponse>> futureResult = futuresSequence.map(new Mapper<Iterable<BulkResponse>, Collection<BulkResponse>>(){
            @Override
            public Collection<BulkResponse> apply(Iterable<BulkResponse> iterable) {
                Collection<BulkResponse> collection = []
                iterable.each {
                    collection << it
                }
                collection
            }
        }, ec)

        futureResult
    }

    Publisher<RiverItem> publisher(RiverConfig config){
        new ObservableToPublisherAdapter<RiverItem>(
                Observable.defer(
                        {
                            Observable.merge(
                                    loadRivers().collect {river ->
                                        river.exportCatalogItemsAsRiverItems(config)
                                    }
                            ).distinct(
                                    { RiverItem item -> item.key }as Func1<RiverItem, String>
                            )
                        }as Func0<Observable<RiverItem>>
                )
        )
    }
}
