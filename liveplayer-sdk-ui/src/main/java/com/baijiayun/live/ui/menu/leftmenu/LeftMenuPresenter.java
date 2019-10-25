package com.baijiayun.live.ui.menu.leftmenu;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.livecore.utils.LPRxUtils;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;


/**
 * Created by Shubo on 2017/2/15.
 */

public class LeftMenuPresenter implements LeftMenuContract.Presenter {

    private LiveRoomRouterListener routerListener;
    private LeftMenuContract.View view;
    private boolean isScreenCleared = false;
    private Disposable disposableOfQuestionQueue;

    public LeftMenuPresenter(LeftMenuContract.View view) {
        this.view = view;
    }

    @Override
    public void clearScreen() {
//        isScreenCleared = !isScreenCleared;
//        view.notifyClearScreenChanged(isScreenCleared);
//        if (isScreenCleared) routerListener.clearScreen();
//        else routerListener.unClearScreen();
    }

    @Override
    public void showMessageInput() {
        routerListener.navigateToMessageInput();
    }

    @Override
    public boolean isScreenCleared() {
        return isScreenCleared;
    }

    @Override
    public boolean isAllForbidden() {
        return !routerListener.isTeacherOrAssistant() && !routerListener.isGroupTeacherOrAssistant() && routerListener.getLiveRoom().getForbidStatus(LPConstants.LPForbidChatType.TYPE_ALL);
    }

    @Override
    public void showHuiyinDebugPanel() {
        routerListener.showHuiyinDebugPanel();
    }

    @Override
    public void showStreamDebugPanel() {
        routerListener.showStreamDebugPanel();
    }

    @Override
    public void showCopyLogDebugPanel() {
        routerListener.showCopyLogDebugPanel();
    }

    @Override
    public boolean isEnableLiveQuestionAnswer() {
        return routerListener.getLiveRoom().getPartnerConfig().enableLiveQuestionAnswer == 1
                && !routerListener.getLiveRoom().isWWWEnvironment();
    }

    @Override
    public void showQuestionAnswer() {
        routerListener.showQuestionAnswer(true);
    }

    @Override
    public IUserModel getCurrentUser() {
        return routerListener.getLiveRoom().getCurrentUser();
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        routerListener = liveRoomRouterListener;
    }

    @Override
    public void subscribe() {
        disposableOfQuestionQueue = routerListener.getLiveRoom().getObservableOfQuestionQueue()
                .subscribe(lpQuestionPullResItems -> {
                    if (!lpQuestionPullResItems.isEmpty() && !routerListener.isQuestionAnswerShow()) {
                        view.showQuestionAnswerInfo(true);
                    }
                });

        if (routerListener.getLiveRoom().isAudition())
            view.setAudition();
    }

    @Override
    public void unSubscribe() {

    }

    @Override
    public void setRemarksEnable(boolean isEnable) {
        routerListener.setRemarksEnable(isEnable);
    }

    @Override
    public void destroy() {
        LPRxUtils.dispose(disposableOfQuestionQueue);
        routerListener = null;
        view = null;
    }
}
