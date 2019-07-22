package com.baijiayun.live.ui.announcement;

import android.support.annotation.StringRes;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.livecore.models.imodels.IAnnouncementModel;

/**
 * Created by Shubo on 2017/4/19.
 */

public interface AnnouncementContract {

    int STATUS_CHECKED_SAVED = 0;
    int STATUS_CHECKED_CANNOT_SAVE = 1;
    int STATUS_CHECKED_CAN_SAVE = 2;

    int TYPE_UI_TEACHERORASSISTANT = 1001;
    int TYPE_UI_GROUPTEACHERORASSISTANT = 1002;
    int TYPE_UI_STUDENT = 1003;

    interface View extends BaseView<Presenter> {

        void editButtonEnable(boolean enable, @StringRes int stringRes);

        void showCurrUI(int type, int groupId);

        void setNoticeInfo(IAnnouncementModel iAnnouncementModel);

        void showBlankTips();
    }

    interface Presenter extends BasePresenter {
        void saveAnnouncement(String text, String url);

        void checkInput(String text, String url);

        void switchUI();

        /**
         * 是否是分组
         * @return      true 分组     false老师
         */
        boolean isGrouping();

        LiveRoomRouterListener getRouter();
    }
}
