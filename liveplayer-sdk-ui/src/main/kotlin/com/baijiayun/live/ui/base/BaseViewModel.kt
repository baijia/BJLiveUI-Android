package com.baijiayun.live.ui.base

import android.arch.lifecycle.ViewModel
import android.support.annotation.CallSuper
import io.reactivex.Flowable
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

/**
 *
 * Created by Shubo on 2019-10-10.
 */

abstract class BaseViewModel : ViewModel() {

    protected val compositeDisposable: CompositeDisposable = CompositeDisposable()

    abstract fun subscribe()

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }

    abstract inner class DisposingObserver<T> : Observer<T> {

        @CallSuper
        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
        }

        override fun onComplete() {

        }
    }
}