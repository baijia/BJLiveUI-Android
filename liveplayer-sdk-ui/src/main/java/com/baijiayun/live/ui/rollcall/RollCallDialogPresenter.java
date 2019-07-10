package com.baijiayun.live.ui.rollcall;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.livecore.listener.OnPhoneRollCallListener;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created by wangkangfei on 17/5/31.
 */

public class RollCallDialogPresenter implements RollCallDialogContract.Presenter {
    private LiveRoomRouterListener routerListener;
    private RollCallDialogContract.View view;
    private Disposable countDownSubscription;
    private int maxTime;
    private OnPhoneRollCallListener.RollCall rollCallListener;

    public RollCallDialogPresenter(RollCallDialogContract.View view) {
        this.view = view;
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        this.routerListener = liveRoomRouterListener;
    }

    public void setRollCallInfo(int maxTime, OnPhoneRollCallListener.RollCall rollCallConfirmListener) {
        this.maxTime = maxTime;
        this.rollCallListener = rollCallConfirmListener;
        subscribe();
    }

    @Override
    public void subscribe() {
        if (countDownSubscription == null) {
            countDownSubscription = Observable.interval(0, 1000, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(Long aLong) {
                            int timePassed = aLong.intValue();
                            if (timePassed >= maxTime) {
                                view.timerDown(0);
                            } else {
                                view.timerDown(maxTime - timePassed);
                            }
                        }
                    });
        }
    }

    @Override
    public void unSubscribe() {
        if (countDownSubscription != null && !countDownSubscription.isDisposed()) {
            countDownSubscription.dispose();
            countDownSubscription = null;
        }
    }

    @Override
    public void destroy() {
        routerListener = null;
        view = null;
    }

    @Override
    public void rollCallConfirm() {
        if (rollCallListener != null) {
            rollCallListener.call();
        }
    }

    @Override
    public void timeOut() {
        if (view != null)
            view.timeOutSoDismiss();
    }
}
