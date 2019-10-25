package com.baijiayun.live.ui.menu.pptleftmenu;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;

/**
 * Created by wangkangfei on 17/5/4.
 */

public interface PPTLeftContract {
    interface View extends BaseView<Presenter> {

    }

    interface Presenter extends BasePresenter {
        void clearPPTAllShapes();
    }
}
