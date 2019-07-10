package com.baijiayun.live.ui.rollcall;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;

/**
 * Created by wangkangfei on 17/5/31.
 */

public interface RollCallDialogContract {

    interface View extends BaseView<Presenter> {
        void timerDown(int time);

        void timeOutSoDismiss();
    }

    interface Presenter extends BasePresenter {
        void rollCallConfirm();

        void timeOut();
    }
}
