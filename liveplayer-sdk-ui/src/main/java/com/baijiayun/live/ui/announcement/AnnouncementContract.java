package com.baijiayun.live.ui.announcement;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;

/**
 * Created by Shubo on 2017/4/19.
 */

interface AnnouncementContract {

    int STATUS_CHECKED_SAVED = 0;

    int STATUS_CHECKED_CANNOT_SAVE = 1;

    int STATUS_CHECKED_CAN_SAVE = 2;

    interface View extends BaseView<Presenter> {
        void showTeacherView();

        void showStudentView();

        void showAnnouncementText(String text);

        void showAnnouncementUrl(String url);

        void showCheckStatus(int status);
    }

    interface Presenter extends BasePresenter {
        void saveAnnouncement(String text, String url);

        void checkInput(String text, String url);
    }
}
