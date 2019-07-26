package com.baijiahulian.live.ui.rollcall;

import com.baijiahulian.live.ui.activity.LiveRoomRouterListener;
import com.baijiahulian.livecore.listener.OnPhoneRollCallListener;
import com.baijiahulian.livecore.utils.LPErrorPrintSubscriber;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by wangkangfei on 17/5/31.
 */

public class RollCallDialogPresenter implements RollCallDialogContract.Presenter {
    private LiveRoomRouterListener routerListener;
    private RollCallDialogContract.View view;
    private Subscription countDownSubscription;
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
                    .subscribe(new LPErrorPrintSubscriber<Long>() {
                        @Override
                        public void call(Long aLong) {
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
        if (countDownSubscription != null && !countDownSubscription.isUnsubscribed()) {
            countDownSubscription.unsubscribe();
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
        view.timeOutSoDismiss();
    }
}
