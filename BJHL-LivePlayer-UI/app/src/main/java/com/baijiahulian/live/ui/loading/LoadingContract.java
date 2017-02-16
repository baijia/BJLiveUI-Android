package com.baijiahulian.live.ui.loading;

import com.baijiahulian.live.ui.BasePresenter;
import com.baijiahulian.live.ui.BaseView;
import com.baijiahulian.livecore.context.LPError;
import com.baijiahulian.livecore.launch.LPLaunchListener;

/**
 * Created by Shubo on 2017/2/14.
 */

interface LoadingContract {
    interface View extends BaseView<Presenter> {
        void showLoadingSteps(int currentStep, int totalSteps);

        void showLaunchError(LPError lpError);
    }

    interface Presenter extends BasePresenter {
        LPLaunchListener getLaunchListener();

        String getCode();

        String getName();
    }
}
