package com.baijiayun.live.ui.users;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.livecore.models.LPGroupItem;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.livecore.models.roomresponse.LPResRoomGroupInfoModel;
import com.baijiayun.livecore.utils.LPLogger;

import java.util.ArrayList;
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
    private Disposable mSubscriptionGroupInfo;
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
                .getOnlineUserVM()
                .getObservableOfOnLineUserCount()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) {
                        view.notifyUserCountChange(routerListener.getLiveRoom().getOnlineUserVM().getAllCount());
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
                        view.notifyUserCountChange(routerListener.getLiveRoom().getOnlineUserVM().getAllCount());
                    }
                });

        mSubscriptionGroupInfo = routerListener.getLiveRoom().getOnlineUserVM().getObservableOfOnGroupItem()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<LPGroupItem>>() {
                    @Override
                    public void accept(List<LPGroupItem> lpGroupItems) throws Exception {
                        view.notifyGroupData(lpGroupItems);
                        view.notifyUserCountChange(routerListener.getLiveRoom().getOnlineUserVM().getAllCount());
                    }
                });

        routerListener.getLiveRoom().getOnlineUserVM().requestGroupInfoReq();
        view.notifyUserCountChange(routerListener.getLiveRoom().getOnlineUserVM().getAllCount());

        //首次进入获取刷新
        loadMore(-1);
    }

    @Override
    public void unSubscribe() {
        RxUtils.dispose(subscriptionOfUserCountChange);
        RxUtils.dispose(subscriptionOfUserDataChange);
        RxUtils.dispose(mSubscriptionGroupInfo);
    }

    @Override
    public void destroy() {
        view = null;
        routerListener = null;
    }

    @Override
    public void updateGroupInfo(LPGroupItem item) {
        routerListener.getLiveRoom().getOnlineUserVM().loadMoreUser(item.id);
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
    public void loadMore(int groupId) {
        isLoading = true;
        routerListener.getLiveRoom().getOnlineUserVM().loadMoreUser(groupId);
    }

    @Override
    public boolean isLoading() {
        return isLoading;
    }

    @Override
    public boolean isGroup() {
        if (routerListener.getLiveRoom().getOnlineUserVM().enableGroupUserPublic())
            return true;
        return false;
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

    @Override
    public int getGroupId() {
        return routerListener.getLiveRoom().getCurrentUser().getGroup();
    }
}
