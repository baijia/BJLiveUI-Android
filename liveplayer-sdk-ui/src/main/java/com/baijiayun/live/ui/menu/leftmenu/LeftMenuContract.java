package com.baijiayun.live.ui.menu.leftmenu;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.livecore.models.imodels.IUserModel;

/**
 * Created by Shubo on 2017/2/15.
 */

interface LeftMenuContract {
    interface View extends BaseView<Presenter> {
        void notifyClearScreenChanged(boolean isCleared);

        //1 webrtc 2avsdk
        void showDebugBtn(int type);

        void showQuestionAnswerInfo(boolean showRed);

        void setAudition();
    }

    interface Presenter extends BasePresenter {
        void clearScreen();

        void showMessageInput();

        boolean isScreenCleared();

        /**
         * 全体禁言状态
         */
        boolean isAllForbidden();

        void showHuiyinDebugPanel();

        void showStreamDebugPanel();

        void showCopyLogDebugPanel();

        boolean isEnableLiveQuestionAnswer();

        void showQuestionAnswer();

        IUserModel getCurrentUser();

        void setRemarksEnable(boolean isEnable);
    }
}
