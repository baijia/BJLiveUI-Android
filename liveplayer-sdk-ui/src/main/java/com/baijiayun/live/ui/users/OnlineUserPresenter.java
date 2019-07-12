package com.baijiayun.live.ui.users;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.livecore.models.imodels.IUserModel;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;


/**
 * Created by Shubo on 2017/4/6.
 */

public class OnlineUserPresenter implements OnlineUserContract.Presenter {

    private OnlineUserContract.View view;
    private LiveRoomRouterListener routerListener;
    private Disposable subscriptionOfUserCountChange, subscriptionOfUserDataChange;
    private volatile boolean isLoading = false;

    public OnlineUserPresenter(OnlineUserContract.View view) {
        this.view = view;
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        routerListener = liveRoomRouterListener;
    }

    @Override
    public void subscribe() {
        subscriptionOfUserCountChange = routerListener.getLiveRoom()
                .getObservableOfUserNumberChange()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) {
                        view.notifyUserCountChange(integer);
                    }
                });
        subscriptionOfUserDataChange = routerListener.getLiveRoom()
                .getOnlineUserVM()
                .getObservableOfOnlineUser()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<IUserModel>>() {
                    @Override
                    public void accept(List<IUserModel> iUserModels) {
                        // iUserModels == null   no more data
                        if (isLoading)
                            isLoading = false;
                        view.notifyDataChanged();
                    }
                });
        view.notifyUserCountChange(routerListener.getLiveRoom().getCurrentUserCount());
    }

    @Override
    public void unSubscribe() {
        RxUtils.dispose(subscriptionOfUserCountChange);
        RxUtils.dispose(subscriptionOfUserDataChange);
    }

    @Override
    public void destroy() {
        view = null;
        routerListener = null;
    }

    @Override
    public int getCount() {
        int count;
        try {
            count = routerListener.getLiveRoom().getOnlineUserVM().getUserCount();
        } catch (Exception e) {
            count = 1;
        }
        return isLoading ? count + 1: count;
    }

    @Override
    public IUserModel getUser(int position) {
        if (!isLoading)
            return routerListener.getLiveRoom().getOnlineUserVM().getUser(position);
        IUserModel iUserModel;
        if (position == getCount()) {
            iUserModel = null;
        } else {
            iUserModel = routerListener.getLiveRoom().getOnlineUserVM().getUser(position);
        }
        return iUserModel;
    }

    @Override
    public void loadMore() {
        isLoading = true;
        routerListener.getLiveRoom().getOnlineUserVM().loadMoreUser();
    }

    @Override
    public boolean isLoading() {
        return isLoading;
    }

    @Override
    public String getPresenter() {
        return routerListener.getLiveRoom().getSpeakQueueVM().getPresenter();
    }

    @Override
    public String getAssistantLabel() {
        return routerListener.getLiveRoom().getCustomizeAssistantLabel();
    }

    @Override
    public String getTeacherLabel() {
        return routerListener.getLiveRoom().getCustomizeTeacherLabel();
    }
}
