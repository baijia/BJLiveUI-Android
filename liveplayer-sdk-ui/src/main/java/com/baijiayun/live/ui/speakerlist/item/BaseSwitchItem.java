package com.baijiayun.live.ui.speakerlist.item;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.baijiayun.live.ui.speakerlist.SpeakersContract;
import com.baijiayun.live.ui.utils.RxUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/**
 * Created by Shubo on 2019-07-30.
 */
public abstract class BaseSwitchItem implements Switchable {

    private boolean isInFullScreen = false;
    protected SpeakersContract.Presenter presenter;

    BaseSwitchItem(SpeakersContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public int getPositionInParent() {
        View view = getView();
        ViewParent viewParent = getView().getParent();
        if (viewParent == null)
            return -1;
        return ((ViewGroup) viewParent).indexOfChild(view);
    }

    @Override
    public boolean isInFullScreen() {
        return isInFullScreen;
    }

    @Override
    public void switchToFullScreen() {
        removeSwitchableFromParent(this);
        presenter.getRouterListener().setFullScreenItem(this);
        isInFullScreen = true;
    }

    @Override
    public void switchBackToList() {
        removeSwitchableFromParent(this);
        presenter.getRouterListener().switchBackToList(this);
        isInFullScreen = false;
    }

    private void removeSwitchableFromParent(Switchable switchable) {
        View view = switchable.getView();
        if (view == null) return;
        ViewParent viewParent = view.getParent();
        if (viewParent == null) return;
        ((ViewGroup) viewParent).removeView(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    void registerClickEvent(View view) {
        GestureDetector gestureDetector = new GestureDetector(((Activity) presenter.getRouterListener()), new ClickGestureDetector(this));
        view.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    protected abstract void showOptionDialog();

    private static class ClickGestureDetector extends GestureDetector.SimpleOnGestureListener {

        private WeakReference<BaseSwitchItem> baseSwitchItemWeakReference;

        ClickGestureDetector(BaseSwitchItem switchItem) {
            baseSwitchItemWeakReference = new WeakReference<>(switchItem);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            BaseSwitchItem switchable = baseSwitchItemWeakReference.get();
            if (switchable == null) {
                RxUtils.dispose(subscriptionOfClickable);
                return false;
            }
            if (clickableCheck()) {
                return false;
            }
            if (!switchable.isInFullScreen()) {
                switchable.showOptionDialog();
            } else {
                // clear screen
                if (((Activity) switchable.presenter.getRouterListener()).getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    switchable.presenter.getRouterListener().switchClearScreen();
                }
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            BaseSwitchItem switchable = baseSwitchItemWeakReference.get();
            if (switchable == null) {
                RxUtils.dispose(subscriptionOfClickable);
                return false;
            }
            if (clickableCheck()) {
                return false;
            }

            if (!switchable.isInFullScreen()) {
                if (switchable instanceof Playable && ((Playable) switchable).isVideoStreaming()) {
                    switchable.presenter.getRouterListener().getFullScreenItem().switchBackToList();
                    switchable.switchToFullScreen();
                }
            }
            return super.onDoubleTap(e);
        }

        Disposable subscriptionOfClickable;

        private boolean clickableCheck() {
            if (subscriptionOfClickable != null && !subscriptionOfClickable.isDisposed()) {
                return true;
            }
            subscriptionOfClickable = Observable.timer(1, TimeUnit.SECONDS).subscribe(aLong -> RxUtils.dispose(subscriptionOfClickable));
            return false;
        }
    }
}
