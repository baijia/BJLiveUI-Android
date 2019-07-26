package com.baijia.live.app;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by Shubo on 2017/6/19.
 */

public class Test {
    public static void main(String[] args) {

        Observable<String> obs = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {

//                SpscLinkedAtomicQueue<Object> q = new SpscLinkedAtomicQueue<>();
//                q.addAll(new ArrayList<>(1000));
//                QueuedProducer<String> p = new QueuedProducer<>(subscriber);
//
//                subscriber.setProducer(p);

                if (!subscriber.isUnsubscribed()) {
                    for (int i = 0; i < 128; i++) {
                        System.out.println("send:" + i);
                        subscriber.onNext(i + " " );
//                        if (i % 40 == 0)
//                            try {
//                                Thread.sleep(2000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
                    }
                    subscriber.onCompleted();
                }
            }
        });

        Subscriber<String> subscriber = new Subscriber<String>() {
            @Override
            public void onCompleted() {
                System.out.println("complete");
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(String s) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(s);
            }

            @Override
            public void onStart() {
                super.onStart();
            }
        };

        obs
//                .onBackpressureBuffer()
                .observeOn(Schedulers.newThread())
                .subscribe(subscriber);

        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
