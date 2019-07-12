package com.baijiayun.live.ui.announcement.modelui;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.livecore.models.imodels.IAnnouncementModel;

/**
 *  公告/通知编辑
 *  panzq
 *  20190708
 */
public class EditAnnContract {


    interface View extends BaseView<Presenter> {

        NoticeInfo getNoticeInfo();

        void initInfo(IAnnouncementModel iAnnModel);
    }

    interface Presenter extends BasePresenter{


    }

    public interface OnAnnEditListener {

        /**
         * 返回
         */
        void cannel();

        /**
         *  发布错误
         */
        void onError();

        /**
         * 发布成功
         */
        void Success();
    }
}
