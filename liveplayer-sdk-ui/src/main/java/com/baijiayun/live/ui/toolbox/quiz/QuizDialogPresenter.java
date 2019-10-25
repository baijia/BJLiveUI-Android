package com.baijiayun.live.ui.toolbox.quiz;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;

/**
 * Created by wangkangfei on 17/5/31.
 */
public class QuizDialogPresenter implements QuizDialogContract.Presenter {
    private QuizDialogContract.View view;
    private LiveRoomRouterListener routerListener;

    public QuizDialogPresenter(QuizDialogContract.View view) {
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
            routerListener.dismissQuizDlg();
        routerListener = null;
    }

    @Override
    public void submitAnswer(String submitContent) {
        routerListener.getLiveRoom().getQuizVM().sendSubmit(submitContent);
    }

    @Override
    public void sendCommonRequest(String request) {
        routerListener.getLiveRoom().sendJSCommonRoomRequest(request);
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
        routerListener.dismissQuizDlg();
    }

    @Override
    public boolean checkRouterNull() {
        return routerListener == null;
    }
}
