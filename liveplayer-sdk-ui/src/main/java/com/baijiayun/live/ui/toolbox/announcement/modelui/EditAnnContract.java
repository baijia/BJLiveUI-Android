package com.baijiayun.live.ui.toolbox.announcement.modelui;

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

        /**
         * 设置标题
         * @param titleType     1   公告
         *                      2   通知
         */
        void setTitle(int titleType);
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
