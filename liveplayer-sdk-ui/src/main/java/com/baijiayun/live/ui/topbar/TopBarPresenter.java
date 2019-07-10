package com.baijiayun.live.ui.topbar;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;

import static com.baijiayun.live.ui.utils.Precondition.checkNotNull;

/**
 * Created by Shubo on 2017/2/13.
 */

public class TopBarPresenter implements TopBarContract.Presenter {

    private LiveRoomRouterListener routerListener;

    private TopBarContract.View view;

    public TopBarPresenter(TopBarContract.View view) {
        this.view = view;
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        routerListener = liveRoomRouterListener;
    }

    @Override
    public void subscribe() {
        checkNotNull(routerListener);
        view.showHideShare(routerListener.getVisibilityOfShareBtn());
    }

    @Override
    public void unSubscribe() {
    }

    @Override
    public void destroy() {
        routerListener = null;
        view = null;
    }

    @Override
    public void navigateToShare() {
        routerListener.navigateToShare();
    }
}
