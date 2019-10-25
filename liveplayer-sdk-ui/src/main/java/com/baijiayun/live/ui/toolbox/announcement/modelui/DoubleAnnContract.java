package com.baijiayun.live.ui.toolbox.announcement.modelui;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.livecore.models.imodels.IAnnouncementModel;

/**
 * 分组老师/学生的公告/通知显示UI
 */
public class DoubleAnnContract {

    interface View extends BaseView<Presenter> {

        void setType(int type, int groupId);

        void setNoticeInfo(IAnnouncementModel iAnnouncementModel);
    }

    interface Presenter extends BasePresenter {

    }
}
