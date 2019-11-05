package com.baijiayun.live.ui.toolbox.evaluation;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;

/**
 * Created by wangkangfei on 17/5/31.
 */
public class EvaDialogPresenter implements EvaDialogContract.Presenter {
    private EvaDialogContract.View view;
    private LiveRoomRouterListener routerListener;

    public EvaDialogPresenter(EvaDialogContract.View view) {
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
        if (routerListener != null)
            routerListener.dismissEvaDialog();
        routerListener = null;
    }

    @Override
    public void submitAnswer(String submitContent) {
        routerListener.getLiveRoom().getQuizVM().sendSubmit(submitContent);
    }

    @Override
    public void getCurrentUser() {
        view.onGetCurrentUser(routerListener.getLiveRoom().getCurrentUser());
    }

    @Override
    public String getRoomToken() {
        return routerListener.getLiveRoom().getQuizVM().getRoomToken();
    }

    @Override
    public void dismissDlg() {
        routerListener.dismissEvaDialog();
    }

    @Override
    public boolean checkRouterNull() {
        return routerListener == null;
    }

    @Override
    public long getRoomId() {
        return routerListener.getLiveRoom().getRoomId();
    }
}
