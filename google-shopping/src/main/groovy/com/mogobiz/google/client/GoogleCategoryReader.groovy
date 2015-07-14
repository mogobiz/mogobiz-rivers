package com.mogobiz.google.client

import rx.Subscription
import rx.subscriptions.Subscriptions

/**
 *
 * Created by smanciot on 03/06/14.
 */
final class GoogleCategoryReader {

    private GoogleCategoryReader(){}

    static rx.Observable<GoogleCategoryItem> parseCategories(final Locale locale = Locale.getDefault()){

        final String taxonomy = 'taxonomy.' + locale?.getLanguage()?.toLowerCase() + '-' + locale?.getCountry()?.toUpperCase() + '.csv'
        InputStream is = GoogleCategoryReader.class.getResourceAsStream(taxonomy)
        final String text = is?.getText('utf-8')
        rx.Observable.create(new rx.Observable.OnSubscribeFunc<GoogleCategoryItem>() {
            @Override
            Subscription onSubscribe(rx.Observer observer) {
                try{
                    text?.eachLine {String line, int count ->
                        if(line.trim().length() > 0){
                            def tokens = line.trim().split(';').collect {token ->
                                token.trim().length() > 0 ? token.trim() : []
                            }.flatten() as String[]
                            observer.onNext(
                                    new GoogleCategoryItem(
                                            locale:locale,
                                            tokens:tokens
                                    )
                            )
                        }
                    }
                }
                catch(Throwable th){
                    observer.onError(th)
                }

                observer.onCompleted()

                return Subscriptions.empty()
            }
        })
    }

}

class GoogleCategoryItem{
    Locale locale
    String[] tokens
}
