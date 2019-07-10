package com.baijiayun.live.ui.ppt.quickswitchppt;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.utils.RxUtils;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * Created by szw on 17/7/5.
 */

public class SwitchPPTFragmentPresenter implements SwitchPPTContract.Presenter {
    private SwitchPPTContract.View view;
    private LiveRoomRouterListener listener;
    private Disposable subscriptionOfDocListChange;

    public SwitchPPTFragmentPresenter(SwitchPPTContract.View view) {
        this.view = view;
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
        view.setType(!listener.isTeacherOrAssistant(), "0".equals(listener.getLiveRoom().getEnableMultiWhiteboard()));
        view.docListChanged(listener.getLiveRoom().getDocListVM().getDocList());
        view.setIndex();
    }

    @Override
    public void unSubscribe() {
        RxUtils.dispose(subscriptionOfDocListChange);
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
    public boolean addPage() {
        return listener.getLiveRoom().pageAdd();
    }

    @Override
    public boolean delPage(int pageId) {
        return listener.getLiveRoom().pageDel(pageId);
    }

    @Override
    public LiveRoomRouterListener getRoute() {
        return listener;
    }

    @Override
    public void changePage(int page) {
        listener.getLiveRoom().pageChange("0", page);
    }
}
