package com.baijiayun.live.ui.toolbox.timer;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.LPBJTimerModel;
import com.baijiayun.livecore.utils.LPRxUtils;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TimerPresenter implements TimerContract.Presenter {
    private TimerContract.View view;
    private LiveRoomRouterListener routerListener;
    private CompositeDisposable disposables;
    private long remainSeconds;
    private long timeDuration;
    private boolean isCountDown = false;
    private boolean isPause = false;
    private Disposable timeDisposable;
    private LPBJTimerModel lpbjTimerModel;

    @Override
    public void requestTimerStart(long current, long total, boolean isCountDown) {
        routerListener.getLiveRoom().getToolBoxVM().requestBJTimerStart(current, total, isCountDown);
    }

    @Override
    public void requestTimerPause(long current, long total, boolean isCountDown) {
        isPause = true;
        routerListener.getLiveRoom().getToolBoxVM().requestBJTimerPause(current, total, isCountDown);
    }

    @Override
    public void requestTimerEnd() {
        if (routerListener.getLiveRoom().getCurrentUser().getType() == LPConstants.LPUserType.Teacher) {
            routerListener.getLiveRoom().getToolBoxVM().requestBJTimerEnd();
        }
    }

    @Override
    public void closeTimer() {
        routerListener.closeTimer();
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        routerListener = liveRoomRouterListener;
    }

    public void setView(TimerContract.View view) {
        this.view = view;
    }

    @Override
    public void subscribe() {
        disposables = new CompositeDisposable();
        if (routerListener.getLiveRoom().getCurrentUser().getType() != LPConstants.LPUserType.Teacher) {
            view.hideButton();
        }
        if (lpbjTimerModel != null) {
            isPause = false;
            long time = lpbjTimerModel.current;
            if (lpbjTimerModel.isCache) {
                time = lpbjTimerModel.startTimer + lpbjTimerModel.current - System.currentTimeMillis() / 1000;
            }
            if (time > 0) {
                isCountDown = lpbjTimerModel.isCountDown();
                timeDuration = lpbjTimerModel.total;
                remainSeconds = time;
                startTimer();
            }
        }
        if (routerListener.getLiveRoom().getCurrentUser().getType() == LPConstants.LPUserType.Teacher) {
            disposables.add(routerListener.getLiveRoom().getToolBoxVM().getObservableOfBJTimerStart()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(lpbjTimerModel -> {
                        isPause = false;
                        long time = lpbjTimerModel.current;
                        if (lpbjTimerModel.isCache) {
                            time = lpbjTimerModel.startTimer + lpbjTimerModel.current - System.currentTimeMillis() / 1000;
                        }
                        if (time > 0) {
                            isCountDown = lpbjTimerModel.isCountDown();
                            timeDuration = lpbjTimerModel.total;
                            remainSeconds = time;
                            startTimer();
                        }
                    }));
        }
        disposables.add(routerListener.getLiveRoom().getToolBoxVM().getObservableOfBJTimerPause()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lpbjTimerModel -> {
                    isPause = true;
                    view.showTimerPause(isPause);
                }));
        disposables.add(routerListener.getLiveRoom().getToolBoxVM().getObservableOfBJTimerEnd()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(b -> {
                    LPRxUtils.dispose(timeDisposable);
                    view.showTimerEnd();
                }));
    }

    private void startTimer() {
        view.showTimerPause(isPause);
        LPRxUtils.dispose(timeDisposable);
        timeDisposable = Observable.interval(0, 1, TimeUnit.SECONDS, Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(aLong -> {
                    if (isPause || view == null) {
                        return;
                    }
                    if (remainSeconds < 0) {
                        LPRxUtils.dispose(timeDisposable);
                        view.showTimerEnd();
                    }
                    if (remainSeconds >= 0) {
                        long seconds = isCountDown ? remainSeconds : timeDuration - remainSeconds;
                        view.setTimer(seconds);
                        view.showViewState(remainSeconds > 60 || timeDuration < 60);
                        remainSeconds--;
                    }
                });
    }

    @Override
    public void unSubscribe() {
        LPRxUtils.dispose(disposables);
    }

    @Override
    public void destroy() {
        view = null;
    }

    public void setTimerModel(LPBJTimerModel lpbjTimerModel) {
        this.lpbjTimerModel = lpbjTimerModel;
    }
}
