package com.baijiayun.live.ui.toolbox.timer;

import android.util.Log;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.base.RouterViewModel;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.LPBJTimerModel;
import com.baijiayun.livecore.utils.LPRxUtils;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import kotlin.Pair;

public class TimerPresenter implements TimerContract.Presenter {
    public static final String pause_timer = "pause";
    public static final String continue_timer = "continue";
    public static final String start_timer = "start";
    public static final String end_timer = "end";
    public static final String stop_timer = "stop";

    private TimerContract.View view;
    private LiveRoomRouterListener routerListener;
    private RouterViewModel routerViewModel;
    private CompositeDisposable disposables;
    private long remainSeconds = -1;
    private long timeDuration;
    private boolean isCountDown = false;
    private boolean isPause = false;
    private Disposable timeDisposable;
    private LPBJTimerModel lpbjTimerModel;
    private boolean isSetting = false;

    @Override
    public void requestTimerStart(long current, long total, boolean isCountDown) {
        if (isSetting){
            //三分屏设置页，初始化LPBJTimerModel，发送给TimerShowy
            timeDuration = total;
            if (remainSeconds > -1){
                current = remainSeconds;
            }else {
                current = total;
            }
            routerListener.getLiveRoom().getToolBoxVM().requestBJTimerStart(current, total, isCountDown);
            LPBJTimerModel lpbjTimerModel = new LPBJTimerModel();
            lpbjTimerModel.startTimer = System.currentTimeMillis() / 1000;
            lpbjTimerModel.current = current;
            lpbjTimerModel.total = total;
            lpbjTimerModel.action = start_timer;
            lpbjTimerModel.type = isCountDown ? "1" : "0";
            routerViewModel.getShowTimerShowy().setValue(new Pair<>(true, lpbjTimerModel));
            unSubscribe();
        }else {
            routerListener.getLiveRoom().getToolBoxVM().requestBJTimerStart(current, total, isCountDown);
        }
    }

    @Override
    public void requestTimerPause(long current, long total, boolean isCountDown) {
        isPause = true;
        routerListener.getLiveRoom().getToolBoxVM().requestBJTimerPause(remainSeconds, timeDuration, this.isCountDown);
    }

    @Override
    public void requestTimerEnd() {
        if (routerListener.getLiveRoom().getCurrentUser().getType() == LPConstants.LPUserType.Teacher) {
            routerListener.getLiveRoom().getToolBoxVM().requestBJTimerEnd();
            if (isSetting){
                resetData();
                routerViewModel.getShowTimer().setValue(new Pair<>(false, lpbjTimerModel));
            }
        }
    }

    private void resetData() {
        remainSeconds = -1;
        timeDuration = 0;
        isPause = false;
        isCountDown = false;
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

    public void setRouterViewModel(RouterViewModel routerViewModel){
        this.routerViewModel = routerViewModel;
    }

    public void setIsSetting(boolean isSetting){
        this.isSetting = isSetting;
    }

    public LPBJTimerModel getLPBJTimerModel(){
        //重新进入三分屏设置页时，TimerShowy向设置页发送现在的LPBJTimerModel
        if(lpbjTimerModel == null){
            return new LPBJTimerModel();
        }
        LPBJTimerModel lpbjTimerModel1 = lpbjTimerModel;
        lpbjTimerModel1.action = isPause ? pause_timer : start_timer;
        lpbjTimerModel1.current = remainSeconds;
        return lpbjTimerModel1;
    }

    @Override
    public void subscribe() {
        disposables = new CompositeDisposable();
        if (lpbjTimerModel != null) {
            isPause = pause_timer.equals(lpbjTimerModel.action);
            isCountDown = lpbjTimerModel.isCountDown();
            timeDuration = lpbjTimerModel.total;

            long time = lpbjTimerModel.current;

            if (lpbjTimerModel.isCache && !isPause){
                //计时中收到数据，应用current减去已经历时间
                time -= (System.currentTimeMillis() / 1000 - lpbjTimerModel.startTimer);
                lpbjTimerModel.isCache = false;
            }
            if (isSetting){
                //三分屏设置页UI
                view.setTimer(lpbjTimerModel.total);
                view.showViewState(isCountDown);
                if (isPause){
                    view.setButtonState(continue_timer);
                }else {
                    view.setButtonState(time > 0 ? pause_timer : start_timer);
                }
            }else {
                view.setTimer(isCountDown ? 0 : timeDuration);
                if (time < 0){
                    view.showTimerEnd();
                }
            }

            if (time > 0) {
                remainSeconds = time;
                //无论设置还是showy，同步计时
                startTimer();
            }
        }
        disposables.add(routerListener.getLiveRoom().getToolBoxVM().getObservableOfBJTimerStart()
                .mergeWith(routerListener.getLiveRoom().getToolBoxVM().getObservableOfBJTimerPause())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lpbjTimerModel1 -> {
                    if (lpbjTimerModel1 == null){
                        return;
                    }
                    if (stop_timer.equals(lpbjTimerModel1.action)){
                        LPRxUtils.dispose(timeDisposable);
                        view.showTimerEnd();
                        return;
                    }
                    isPause = pause_timer.equals(lpbjTimerModel1.action);
                    view.showTimerPause(isPause);

                    lpbjTimerModel = lpbjTimerModel1;
                    isCountDown = lpbjTimerModel1.isCountDown();
                    timeDuration = lpbjTimerModel.total;
                    long time = lpbjTimerModel.current;
                    if (lpbjTimerModel.isCache) {
                        time = lpbjTimerModel.startTimer + time - System.currentTimeMillis() / 1000;
                    }
                    if (time > 0) {
                        remainSeconds = time;
                        startTimer();
                    }
                }));
        disposables.add(routerListener.getLiveRoom().getToolBoxVM().getObservableOfBJTimerEnd()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(b -> {

                }));
    }

    private void startTimer() {
        view.showTimerPause(isPause);
        LPRxUtils.dispose(timeDisposable);
        timeDisposable = Observable.interval(0, 1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread()).subscribe(aLong -> {
                    if (view == null) {
                        return;
                    }
                    if (remainSeconds < 0) {
                        LPRxUtils.dispose(timeDisposable);
                        view.showTimerEnd();
                    }
                    if (remainSeconds >= 0) {
                        //设置页不显示变化，仅发送命令
                        if (!isSetting) {
                            long seconds = isCountDown ? remainSeconds : timeDuration - remainSeconds;
                            view.setTimer(seconds);
                            view.setButtonState(remainSeconds > 0 ? start_timer : end_timer);
                            view.showViewState(remainSeconds >= 60 || timeDuration <= 60);
                        }
                        remainSeconds -= isPause ? 0 : 1;
                    }
                });
    }

    @Override
    public void unSubscribe() {
        LPRxUtils.dispose(disposables);
        LPRxUtils.dispose(timeDisposable);
        lpbjTimerModel = null;
    }

    @Override
    public void destroy() {
        view = null;
    }

    private int id = 0;
    public void setId(int id){
        this.id = id;
    }

    public void setTimerModel(LPBJTimerModel lpbjTimerModel) {
        this.lpbjTimerModel = lpbjTimerModel;
    }
}
