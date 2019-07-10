package com.baijiayun.live.ui.chat.privatechat;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.imodels.IUserModel;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * Created by yangjingming on 2018/1/16.
 */

public class ChatUsersPresenter implements ChatUsersContract.Presenter {

    private ChatUsersContract.View view;
    private LiveRoomRouterListener routerListener;
    private Disposable subscriptionOfUserCountChange, subscriptionOfUserDataChange;
    private boolean isLoading = false;
    private List<IUserModel> iChatUserModels;

    public ChatUsersPresenter(ChatUsersContract.View view) {
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
                .subscribe(integer -> {
                });
        subscriptionOfUserDataChange = routerListener.getLiveRoom()
                .getOnlineUserVM()
                .getObservableOfOnlineUser()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(iUserModels -> {
                    // iUserModels == null   no more data
                    onChatUsersChanged();
                    if (isLoading)
                        isLoading = false;
                    if (!isPrivateChatUserAvailable()) {
                        routerListener.onPrivateChatUserChange(null);
                        view.showPrivateChatLabel(null);
                    }
                    view.notifyDataChanged();
                });
        view.notifyDataChanged();
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


    private void onChatUsersChanged() {
        if (routerListener.isTeacherOrAssistant() || routerListener.isGroupTeacherOrAssistant()) {
            iChatUserModels = new ArrayList<>();
            int size = routerListener.getLiveRoom().getOnlineUserVM().getUserCount();
            for (int i = 0; i < size; i++) {
                IUserModel iUserModel = routerListener.getLiveRoom().getOnlineUserVM().getUser(i);
                if (!iUserModel.getUserId().equals(routerListener.getLiveRoom().getCurrentUser().getUserId()))
                    iChatUserModels.add(iUserModel);
            }
        }else {
            iChatUserModels = new ArrayList<>();
            int size = routerListener.getLiveRoom().getOnlineUserVM().getUserCount();
            if (routerListener.getLiveRoom().getTeacherUser() != null)
                iChatUserModels.add(routerListener.getLiveRoom().getTeacherUser());
            for (int i = 0; i < size; i++) {
                IUserModel userModel = routerListener.getLiveRoom().getOnlineUserVM().getUser(i);
                if (userModel.getUserId().equals(routerListener.getLiveRoom().getCurrentUser().getUserId())) continue;
                if (userModel.getType().equals(LPConstants.LPUserType.Assistant) ) {
                    iChatUserModels.add(userModel);
                }
            }
        }
        if (iChatUserModels.isEmpty()) {
            view.privateChatUserChanged(true);
        } else {
            view.privateChatUserChanged(false);
        }
    }


    private boolean isPrivateChatUserAvailable() {
        return iChatUserModels.contains(routerListener.getPrivateChatUser());
    }

    @Override
    public void chooseOneToChat(String chatName, boolean isEnter) {
        view.showPrivateChatLabel(chatName);
    }

    @Override
    public void setPrivateChatUser(IUserModel iUserModel) {
        routerListener.onPrivateChatUserChange(iUserModel);
        view.notifyDataChanged();
    }

    @Override
    public IUserModel getPrivateChatUser() {
        return routerListener.getPrivateChatUser();
    }

    @Override
    public int getCount() {
        int count = iChatUserModels.size();
        return isLoading ? count + 1 : count;
    }

    @Override
    public IUserModel getUser(int position) {
        if (!isLoading) {
            return iChatUserModels.get(position);
        }
        IUserModel iUserModel;
        if (iChatUserModels.size() == position) {
            iUserModel = null;
        } else {
            iUserModel = iChatUserModels.get(position);
        }
        return iUserModel;

    }

    @Override
    public void loadMore() {
        isLoading = true;
        routerListener.getLiveRoom().getOnlineUserVM().loadMoreUser();
        onChatUsersChanged();
    }

    @Override
    public boolean isLoading() {
        return isLoading;
    }

    @Override
    public String getTeacherLabel() {
        return routerListener.getLiveRoom().getCustomizeTeacherLabel();
    }

    @Override
    public String getAssistantLabel() {
        return routerListener.getLiveRoom().getCustomizeAssistantLabel();
    }
}

