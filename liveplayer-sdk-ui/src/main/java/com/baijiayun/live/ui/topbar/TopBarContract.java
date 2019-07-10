package com.baijiayun.live.ui.topbar;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;

/**
 * Created by Shubo on 2017/2/13.
 */

interface TopBarContract {

    interface View extends BaseView<Presenter> {

        void showHideShare(boolean show);
    }

    interface Presenter extends BasePresenter {

        void navigateToShare();
    }

}
