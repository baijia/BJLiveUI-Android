package com.baijiayun.live.ui.utils;

import android.os.Looper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.view.View;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;

/**
 * Created by Shubo on 2017/2/13.
 */

public class RxUtils {

    public static void dispose(Disposable disposable) {
        if (disposable != null && !disposable.isDisposed())
            disposable.dispose();
    }

    @CheckResult
    @NonNull
    public static Observable<Integer> clicks(@NonNull View view) {
        checkNotNull(view, "view == null");
        return Observable.create(new RxUtils.ViewClickOnSubscribe(view));
    }

    private static class ViewClickOnSubscribe implements ObservableOnSubscribe<Integer> {
        final View view;

        ViewClickOnSubscribe(View view) {
            this.view = view;
        }

        @Override
        public void subscribe(ObservableEmitter<Integer> observableEmitter) throws Exception {
            checkUiThread();

            View.OnClickListener listener = v -> {
                if (!observableEmitter.isDisposed()) {
                    observableEmitter.onNext(view.getId());
                }
            };
            view.setOnClickListener(listener);

            observableEmitter.setCancellable(() -> {
                view.setOnClickListener(null);
            });
        }
    }

    public static <T> T checkNotNull(T value, String message) {
        if (value == null) {
            throw new NullPointerException(message);
        }
        return value;
    }

    public static void checkUiThread() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalStateException(
                    "Must be called from the main thread. Was: " + Thread.currentThread());
        }
    }
}
