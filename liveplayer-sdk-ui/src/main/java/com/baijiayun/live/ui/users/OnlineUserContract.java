package com.baijiayun.live.ui.users;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.livecore.models.LPGroupItem;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.livecore.models.roomresponse.LPResRoomGroupInfoModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Shubo on 2017/4/5.
 */

public interface OnlineUserContract {

    interface View extends BaseView<Presenter> {
        void notifyDataChanged();

        void notifyNoMoreData();

        void notifyUserCountChange(int count);

        void notifyGroupData(List<LPGroupItem> lpGroupItems);

        void showGroupView(boolean isShow);
    }

    interface Presenter extends BasePresenter {
        int getCount();

        IUserModel getUser(int position);

        void loadMore(int groupId);

        boolean isLoading();

        String getPresenter();

        String getTeacherLabel();

        String getAssistantLabel();

        boolean isGroup();

        void updateGroupInfo(LPGroupItem item);

        int getGroupId();
    }
}
