package com.baijiayun.live.ui.chat.privatechat;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.LPUserModel;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.livecore.utils.LPLogger;

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
        List<IUserModel> userModels = routerListener.getLiveRoom().getOnlineUserVM().getPrivateUser();

        if (iChatUserModels == null)
            iChatUserModels = new ArrayList<>();
        iChatUserModels.clear();

        for (int index = 0; index < userModels.size(); index++) {
            IUserModel model = userModels.get(index);

            if (routerListener.isTeacherOrAssistant()) {
                //大班教室显示左右用户
                iChatUserModels.addAll(userModels);
                break;
            } else if (routerListener.isGroupTeacherOrAssistant()) {
                //分组教师区分显示
                if (routerListener.getLiveRoom().getOnlineUserVM().enableGroupUserPublic()) {
                    //组间可见
                    iChatUserModels.addAll(userModels);
                    break;
                } else {
                    //组内可见
                    if (model.getGroup() == 0 && (model.getType() == LPConstants.LPUserType.Teacher
                            || model.getType() == LPConstants.LPUserType.Assistant)) {
                        //分组0老师/助教
                        iChatUserModels.add(model);
                    } else if (model.getGroup() == routerListener.getLiveRoom().getCurrentUser().getGroup()) {
                        iChatUserModels.add(model);
                    }
                }
            } else {
                //学生区分显示
                if (model.getType() != LPConstants.LPUserType.Teacher && model.getType() != LPConstants.LPUserType.Assistant)
                    continue;

                if (routerListener.getLiveRoom().getOnlineUserVM().enableGroupUserPublic()) {
                    //组间可见
                    iChatUserModels.add(model);
                } else {
                    //本组可见
                    if (model.getGroup() == 0
                            || model.getGroup() == routerListener.getLiveRoom().getCurrentUser().getGroup()) {
                        //分组0老师/助教与当前组
                        iChatUserModels.add(model);
                    }
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
        if (routerListener.getLiveRoom().getOnlineUserVM().enableGroupUserPublic()) {
            routerListener.getLiveRoom().getOnlineUserVM().loadMoreUser(0);
        } else {
            routerListener.getLiveRoom().getOnlineUserVM().loadMoreUser(-1);
        }
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

