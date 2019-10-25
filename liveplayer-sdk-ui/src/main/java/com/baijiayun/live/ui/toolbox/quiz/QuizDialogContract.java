package com.baijiayun.live.ui.toolbox.quiz;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.livecore.models.LPJsonModel;
import com.baijiayun.livecore.models.imodels.IUserModel;

/**
 * Created by wangkangfei on 17/5/31.
 */

public interface QuizDialogContract {

    interface View extends BaseView<Presenter> {
        void onStartArrived(LPJsonModel jsonModel);

        void onEndArrived(LPJsonModel jsonModel);

        void onSolutionArrived(LPJsonModel jsonModel);

        void onQuizResArrived(LPJsonModel jsonModel);

        void onGetCurrentUser(IUserModel userModel);

        void dismissDlg();
    }

    interface Presenter extends BasePresenter {
        void submitAnswer(String submitContent);

        void sendCommonRequest(String request);

        void getCurrentUser();

        String getRoomToken();

        void dismissDlg();

        boolean checkRouterNull();
    }
}
