package com.baijiayun.live.ui.loading;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.livecore.context.LPError;
import com.baijiayun.livecore.context.LiveRoom;
import com.baijiayun.livecore.listener.LPLaunchListener;
import com.baijiayun.livecore.models.imodels.IUserModel;

/**
 * Created by Shubo on 2017/2/14.
 */

interface LoadingContract {
    interface View extends BaseView<Presenter> {
        void showLoadingSteps(int currentStep, int totalSteps);

        void showLaunchError(LPError lpError);

        void setTechSupportVisibility(boolean shouldShow);
    }

    interface Presenter extends BasePresenter {
        LPLaunchListener getLaunchListener();

        String getCode();

        String getName();

        String getAvatar();

        String getSign();

        IUserModel getUser();

        long getRoomId();

        void setLiveRoom(LiveRoom liveRoom);

        boolean isJoinCode();
    }
}
