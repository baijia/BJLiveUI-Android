package com.baijiayun.live.ui.users;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.livecore.models.imodels.IUserModel;

/**
 * Created by Shubo on 2017/4/5.
 */

public interface OnlineUserContract {

    interface View extends BaseView<Presenter> {
        void notifyDataChanged();

        void notifyNoMoreData();

        void notifyUserCountChange(int count);
    }

    interface Presenter extends BasePresenter {
        int getCount();

        IUserModel getUser(int position);

        void loadMore();

        boolean isLoading();

        String getPresenter();

        String getTeacherLabel();

        String getAssistantLabel();
    }
}
