package com.baijiayun.live.ui.speakerlist;

import android.support.annotation.CallSuper;

import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * Created by Shubo on 2019-07-25.
 */
class DisposableHelper {
    private static CompositeDisposable compositeDisposable;

    private static void add(Disposable disposable) {
        getCompositeDisposable().add(disposable);
    }

    static void dispose() {
        getCompositeDisposable().dispose();
    }

    private static CompositeDisposable getCompositeDisposable() {
        if (compositeDisposable == null || compositeDisposable.isDisposed()) {
            compositeDisposable = new CompositeDisposable();
        }
        return compositeDisposable;
    }

    private DisposableHelper() {
    }

    abstract static class DisposingObserver<T> implements Observer<T> {

        @Override
        @CallSuper
        public void onSubscribe(Disposable d) {
            DisposableHelper.add(d);
        }

        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
        }

        @Override
        public void onComplete() {

        }
    }
}
