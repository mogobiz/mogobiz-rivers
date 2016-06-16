/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.google.client

import rx.Subscriber
import rx.Subscription

/**
 *
 */
final class GoogleCategoryReader {

    private GoogleCategoryReader(){}

    static rx.Observable<GoogleCategoryItem> parseCategories(final Locale locale = Locale.getDefault()){

        final String taxonomy = 'taxonomy.' + locale?.getLanguage()?.toLowerCase() + '-' + locale?.getCountry()?.toUpperCase() + '.csv'
        InputStream is = GoogleCategoryReader.class.getResourceAsStream(taxonomy)
        final String text = is?.getText('utf-8')
        rx.Observable.create(new rx.Observable.OnSubscribe<GoogleCategoryItem>() {
            @Override
            void call(Subscriber<? super GoogleCategoryItem> subscriber) {
                try{
                    def subscription = new InnerSubscription()
                    subscriber.add(subscription)
                    subscriber.onStart()
                    text?.eachLine {String line, int count ->
                        if(line.trim().length() > 0){
                            def tokens = line.trim().split(';').collect {token ->
                                token.trim().length() > 0 ? token.trim() : []
                            }.flatten() as String[]
                            if(!subscription.isUnsubscribed()){
                                subscriber.onNext(
                                        new GoogleCategoryItem(
                                                locale:locale,
                                                tokens:tokens
                                        )
                                )
                            }
                        }
                    }
                }
                catch(Throwable th){
                    subscriber.onError(th)
                }

                subscriber.onCompleted()

            }
        })
    }

}

class GoogleCategoryItem{
    Locale locale
    String[] tokens
}

class InnerSubscription implements Subscription{
    private boolean unsubscribed = false;
    @Override
    void unsubscribe() {
        unsubscribed = true
    }

    @Override
    boolean isUnsubscribed() {
        return unsubscribed
    }
}
