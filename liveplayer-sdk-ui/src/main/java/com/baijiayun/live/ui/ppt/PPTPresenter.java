package com.baijiayun.live.ui.ppt;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;

/**
 * Created by Shubo on 2017/2/18.
 */

public class PPTPresenter implements PPTContract.Presenter {

    private PPTContract.View view;
    private LiveRoomRouterListener routerListener;
    private boolean isScreenCleared = false;

    public PPTPresenter(PPTContract.View view) {
        this.view = view;
    }

    @Override
    public void clearScreen() {
        if (!routerListener.isPPTMax()) return;
        isScreenCleared = !isScreenCleared;
        if (isScreenCleared) routerListener.clearScreen();
        else routerListener.unClearScreen();
    }

    @Override
    public void showQuickSwitchPPTView(int currentIndex, int maxIndex) {
        routerListener.navigateToQuickSwitchPPT(currentIndex, maxIndex);
    }

    @Override
    public void updateQuickSwitchPPTView(int maxIndex) {
        routerListener.updateQuickSwitchPPTMaxIndex(maxIndex);
    }

    @Override
    public void showPPTLoadError(int errorCode, String description) {
        routerListener.showPPTLoadErrorDialog(errorCode, description);
    }

    @Override
    public LiveRoomRouterListener getRouter() {
        return routerListener;
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        routerListener = liveRoomRouterListener;
    }

    @Override
    public void subscribe() {

    }

    @Override
    public void unSubscribe() {

    }

    @Override
    public void destroy() {
        view = null;
    }
}
