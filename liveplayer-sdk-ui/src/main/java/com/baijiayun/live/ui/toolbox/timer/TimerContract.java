package com.baijiayun.live.ui.toolbox.timer;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;

public interface TimerContract {
    interface Presenter extends BasePresenter{
        void requestTimerStart(long current, long total, boolean isCountDown);

        void requestTimerPause(long current, long total, boolean isCountDown);

        void requestTimerEnd();

        void closeTimer();
    }
    interface View extends BaseView<Presenter>{

        void setTimer(long remainSeconds);

        void showViewState(boolean enable);

        void hideButton();

        void showTimerEnd();

        void showTimerPause(boolean isPause);
    }
}
