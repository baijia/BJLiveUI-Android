package com.baijiayun.live.ui.menu.pptleftmenu;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;

/**
 * Created by wangkangfei on 17/5/4.
 */

public class PPTLeftPresenter implements PPTLeftContract.Presenter {
    private PPTLeftContract.View view;
    private LiveRoomRouterListener routerListener;

    public PPTLeftPresenter(PPTLeftContract.View view) {
        this.view = view;
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        this.routerListener = liveRoomRouterListener;
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
        routerListener = null;
    }

    @Override
    public void clearPPTAllShapes() {
        routerListener.clearPPTAllShapes();
    }
}
