package com.baijiayun.live.ui.ppt;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;

/**
 * Created by Shubo on 2017/2/18.
 */

interface PPTContract {

    interface View extends BaseView<Presenter> {

    }

    interface Presenter extends BasePresenter {
        void clearScreen();

        void showQuickSwitchPPTView(int currentIndex, int maxIndex);

        void updateQuickSwitchPPTView(int maxIndex);

        void showPPTLoadError(int errorCode, String description);

        LiveRoomRouterListener getRouter();
    }
}
