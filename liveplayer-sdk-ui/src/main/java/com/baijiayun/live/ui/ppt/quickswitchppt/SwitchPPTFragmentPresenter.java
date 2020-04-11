package com.baijiayun.live.ui.ppt.quickswitchppt;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.LPAdminAuthModel;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.livecore.ppt.listener.OnPPTStateListener;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * Created by szw on 17/7/5.
 */

public class SwitchPPTFragmentPresenter implements SwitchPPTContract.Presenter {
    private SwitchPPTContract.View view;
    private LiveRoomRouterListener listener;
    private Disposable subscriptionOfDocListChange, subscriptionOfPageChange;
    private boolean enableMultiWhiteboard;

    public SwitchPPTFragmentPresenter(SwitchPPTContract.View view, boolean enableMultiWhiteboard) {
        this.view = view;
        this.enableMultiWhiteboard = enableMultiWhiteboard;
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        this.listener = liveRoomRouterListener;
    }

    @Override
    public void subscribe() {
        subscriptionOfDocListChange = listener.getLiveRoom().getDocListVM().getObservableOfDocListChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(docModels -> view.docListChanged(docModels));
        view.setType(!listener.isTeacherOrAssistant(), !enableMultiWhiteboard);
        view.docListChanged(listener.getLiveRoom().getDocListVM().getDocList());
        view.setIndex(-1);

        subscriptionOfPageChange = listener.getLiveRoom().getDocListVM().getObservableOfDocPageIndex()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(index -> {
                    if(view != null){
                        view.setIndex(index);
                    }
                });
    }

    @Override
    public void unSubscribe() {
        RxUtils.dispose(subscriptionOfDocListChange);
        RxUtils.dispose(subscriptionOfPageChange);
    }

    @Override
    public void destroy() {
        listener = null;
        view = null;
    }

    @Override
    public void setSwitchPosition(int position) {
        listener.notifyPageCurrent(position);
    }

    public void notifyMaxIndexChange(int maxIndex) {
        if (view != null)
            view.setMaxIndex(maxIndex);
    }

    @Override
    public void addPage() {
        listener.addPPTWhiteboardPage();
    }

    @Override
    public void delPage(int pageId) {
        listener.deletePPTWhiteboardPage(pageId);
    }

    @Override
    public LiveRoomRouterListener getRoute() {
        return listener;
    }

    @Override
    public void changePage(int page) {
        listener.changePage("0", page);
    }

    @Override
    public boolean canOperateDocumentControl() {
        final IUserModel currentUser = listener.getLiveRoom().getCurrentUser();
        final LPAdminAuthModel adminAuth = listener.getLiveRoom().getAdminAuth();
        if (currentUser == null) {
            return false;
        }
        if (currentUser.getType() == LPConstants.LPUserType.Assistant) {
            return !(adminAuth != null && !adminAuth.documentControl);
        } else if (currentUser.getType() == LPConstants.LPUserType.Teacher) {
            return true;
        } else {
            return !listener.getLiveRoom().getStudentSwitchPPTState();
        }
    }

    @Override
    public void changeDocList() {
        view.docListChanged(listener.getLiveRoom().getDocListVM().getDocList());
    }
}
