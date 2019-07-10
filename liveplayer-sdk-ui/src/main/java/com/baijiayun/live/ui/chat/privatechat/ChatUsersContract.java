package com.baijiayun.live.ui.chat.privatechat;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.livecore.models.imodels.IUserModel;

/**
 * Created by yangjingming on 2018/1/16.
 */

public interface ChatUsersContract {
    interface View extends BaseView<Presenter> {
        void notifyDataChanged();

        void notifyNoMoreData();

        void showPrivateChatLabel(String chatName);

        void initPrivateChatLabel(IUserModel iUserModel);

        void privateChatUserChanged(boolean isEmpty);
    }

    interface Presenter extends BasePresenter {
        int getCount();

        IUserModel getUser(int position);

        void chooseOneToChat(String chatName, boolean isEnter);

        void setPrivateChatUser(IUserModel iUserModel);

        IUserModel getPrivateChatUser();

        void loadMore();

        boolean isLoading();

        String getTeacherLabel();

        String getAssistantLabel();
    }
}
