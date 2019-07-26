package com.baijiahulian.live.ui.leftmenu;

import com.baijiahulian.live.ui.base.BasePresenter;
import com.baijiahulian.live.ui.base.BaseView;

/**
 * Created by Shubo on 2017/2/15.
 */

interface LeftMenuContract {
    interface View extends BaseView<Presenter> {
        void notifyClearScreenChanged(boolean isCleared);

        void showDebugBtn();
    }

    interface Presenter extends BasePresenter {
        void clearScreen();

        void showMessageInput();

        boolean isScreenCleared();

        /**
         * 全体禁言状态
         */
        boolean isAllForbidden();

        /**
         * 自己是否被禁言（student）
         */
        boolean isForbiddenByTeacher();

        void showHuiyinDebugPanel();

        void showStreamDebugPanel();

        void showCopyLogDebugPanel();
    }
}
