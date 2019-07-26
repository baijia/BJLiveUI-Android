package com.baijiahulian.live.ui.leftmenu;

import com.baijiahulian.live.ui.activity.LiveRoomRouterListener;
import com.baijiahulian.livecore.utils.LPErrorPrintSubscriber;

import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by Shubo on 2017/2/15.
 */

public class LeftMenuPresenter implements LeftMenuContract.Presenter {

    private LiveRoomRouterListener routerListener;
    private LeftMenuContract.View view;
    private boolean isScreenCleared = false;
    private boolean isSelfForbidden = false;

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
        return !routerListener.isTeacherOrAssistant() && !routerListener.isGroupTeacherOrAssistant() && routerListener.getLiveRoom().getForbidStatus();
    }

    @Override
    public boolean isForbiddenByTeacher() {
        if (routerListener.isTeacherOrAssistant() || routerListener.isGroupTeacherOrAssistant()) {
            return false;
        }
        return isSelfForbidden;
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
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        routerListener = liveRoomRouterListener;
    }

    @Override
    public void subscribe() {
        routerListener.getLiveRoom().getObservableOfIsSelfChatForbid()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        isSelfForbidden = aBoolean;
                    }
                });
    }

    @Override
    public void unSubscribe() {

    }

    @Override
    public void destroy() {
        routerListener = null;
        view = null;
    }
}
