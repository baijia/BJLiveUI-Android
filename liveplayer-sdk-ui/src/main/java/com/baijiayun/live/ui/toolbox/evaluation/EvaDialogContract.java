package com.baijiayun.live.ui.toolbox.evaluation;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.livecore.models.LPJsonModel;
import com.baijiayun.livecore.models.imodels.IUserModel;

/**
 * Created by wangkangfei on 17/5/31.
 */

public interface EvaDialogContract {

    interface View extends BaseView<Presenter> {
        void onClassEnd(LPJsonModel jsonModel);

        void onGetCurrentUser(IUserModel userModel);

        void dismissDlg();
    }

    interface Presenter extends BasePresenter {
        void submitAnswer(String submitContent);

        void getCurrentUser();

        String getRoomToken();

        void dismissDlg();

        boolean checkRouterNull();

        long getRoomId();
    }
}
